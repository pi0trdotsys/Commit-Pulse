package com.commitpulse.app.github

import com.commitpulse.app.data.DayCommit
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class GraphQLRequest(
    val query: String,
    val variables: Map<String, String>,
)

@Serializable
data class GraphQLResponse(
    val data: ViewerData? = null,
    val errors: List<GraphQLError>? = null,
)

@Serializable
data class GraphQLError(val message: String)

@Serializable
data class ViewerData(val viewer: Viewer)

@Serializable
data class Viewer(
    val login: String,
    val avatarUrl: String,
    val contributionsCollection: ContributionsCollection,
)

@Serializable
data class ContributionsCollection(val contributionCalendar: ContributionCalendar)

@Serializable
data class ContributionCalendar(
    val totalContributions: Int,
    val weeks: List<ContributionWeek>,
)

@Serializable
data class ContributionWeek(val contributionDays: List<ContributionDay>)

@Serializable
data class ContributionDay(val date: String, val contributionCount: Int)

/**
 * Mapuje surowy kalendarz kontrybucji z GitHub GraphQL API na posortowaną chronologicznie
 * historię commitów. Czysta funkcja (bez sieci/Androida) wydzielona z [com.commitpulse.app.github.GitHubRepository.fetchHistory],
 * żeby dało się przetestować, że commity i ich podsumowania liczone są poprawnie.
 */
fun ContributionCalendar.toDayCommits(): List<DayCommit> =
    weeks
        .flatMap { it.contributionDays }
        .map { DayCommit(LocalDate.parse(it.date), it.contributionCount) }
        .sortedBy { it.date }

const val CONTRIBUTIONS_QUERY = """
query(${'$'}from: DateTime!, ${'$'}to: DateTime!) {
  viewer {
    login
    avatarUrl
    contributionsCollection(from: ${'$'}from, to: ${'$'}to) {
      contributionCalendar {
        totalContributions
        weeks {
          contributionDays {
            date
            contributionCount
          }
        }
      }
    }
  }
}
"""
