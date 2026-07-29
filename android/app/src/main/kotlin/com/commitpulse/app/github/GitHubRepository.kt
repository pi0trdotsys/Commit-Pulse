package com.commitpulse.app.github

import android.content.Context
import com.commitpulse.app.data.DayCommit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class GitHubAccount(val login: String, val avatarUrl: String)

sealed class GitHubResult<out T> {
    data class Success<T>(val value: T) : GitHubResult<T>()
    data class Failure(val message: String) : GitHubResult<Nothing>()
}

/** Historia commitów + profil pobierane z GitHub GraphQL API (contributionsCollection). */
class GitHubRepository(context: Context) {
    private val tokenStore = TokenStore(context)

    private val json = Json { ignoreUnknownKeys = true }

    private fun buildApi(token: String): GitHubApi {
        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Accept", "application/vnd.github+json")
                .build()
            chain.proceed(request)
        }
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        return retrofit.create(GitHubApi::class.java)
    }

    fun hasToken(): Boolean = tokenStore.hasToken()

    fun saveToken(token: String) = tokenStore.saveToken(token.trim())

    fun signOut() = tokenStore.clearToken()

    /** Waliduje token i pobiera ~[days] dni historii kontrybucji (max 1 rok wg limitu GraphQL). */
    suspend fun fetchHistory(days: Int = 100, token: String? = null): GitHubResult<Pair<GitHubAccount, List<DayCommit>>> =
        withContext(Dispatchers.IO) {
            val effectiveToken = token ?: tokenStore.getToken()
            if (effectiveToken.isNullOrBlank()) {
                return@withContext GitHubResult.Failure("Brak zapisanego tokenu GitHub.")
            }
            try {
                val api = buildApi(effectiveToken)
                val to = Instant.now()
                val from = to.minusSeconds(days.toLong() * 24 * 3600)
                val formatter = DateTimeFormatter.ISO_INSTANT
                val response = api.graphql(
                    GraphQLRequest(
                        query = CONTRIBUTIONS_QUERY,
                        variables = mapOf(
                            "from" to formatter.format(from),
                            "to" to formatter.format(to),
                        ),
                    ),
                )
                val errors = response.errors
                if (!errors.isNullOrEmpty()) {
                    return@withContext GitHubResult.Failure(errors.joinToString("; ") { it.message })
                }
                val viewer = response.data?.viewer
                    ?: return@withContext GitHubResult.Failure("Nie udało się odczytać odpowiedzi GitHub API.")

                val history = viewer.contributionsCollection.contributionCalendar.weeks
                    .flatMap { it.contributionDays }
                    .map { DayCommit(LocalDate.parse(it.date), it.contributionCount) }
                    .sortedBy { it.date }

                GitHubResult.Success(GitHubAccount(viewer.login, viewer.avatarUrl) to history)
            } catch (e: Exception) {
                GitHubResult.Failure(e.message ?: "Nieznany błąd sieci.")
            }
        }
}
