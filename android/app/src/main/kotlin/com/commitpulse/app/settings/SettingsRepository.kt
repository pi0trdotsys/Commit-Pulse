package com.commitpulse.app.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.commitpulse.app.data.DayCommit
import com.commitpulse.app.data.Palette
import com.commitpulse.app.data.Range
import com.commitpulse.app.data.RefreshInterval
import com.commitpulse.app.data.Surface
import com.commitpulse.app.data.TapAction
import com.commitpulse.app.data.WidgetMode
import com.commitpulse.app.data.WidgetSettings
import com.commitpulse.app.data.toDayCommitList
import com.commitpulse.app.data.toJson
import com.commitpulse.app.github.GitHubAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "commit_pulse_settings")

/** Trwałe ustawienia widgetu + podręczna pamięć ostatnio pobranej historii commitów. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val MODE = stringPreferencesKey("mode")
        val PALETTE = stringPreferencesKey("palette")
        val SURFACE = stringPreferencesKey("surface")
        val GOAL = intPreferencesKey("goal")
        val RANGE = intPreferencesKey("range")
        val REFRESH = stringPreferencesKey("refresh")
        val TAP_ACTION = stringPreferencesKey("tap_action")
        val WEEKLY_DIGEST = booleanPreferencesKey("weekly_digest")
        val DIGEST_DAY = intPreferencesKey("digest_day")
        val DIGEST_HOUR = stringPreferencesKey("digest_hour")
        val ALERT_STREAK = booleanPreferencesKey("alert_streak")
        val ALERT_GOAL = booleanPreferencesKey("alert_goal")
        val ALERT_MILESTONES = booleanPreferencesKey("alert_milestones")

        val HISTORY_JSON = stringPreferencesKey("history_json")
        val ACCOUNT_LOGIN = stringPreferencesKey("account_login")
        val ACCOUNT_AVATAR = stringPreferencesKey("account_avatar")
        val LAST_SYNC_EPOCH = stringPreferencesKey("last_sync_epoch")
        val LAST_ERROR = stringPreferencesKey("last_error")

        val BEST_STREAK_SEEN = intPreferencesKey("best_streak_seen")
        val BEST_DAY_SEEN = intPreferencesKey("best_day_seen")
    }

    val settingsFlow: Flow<WidgetSettings> = context.dataStore.data.map { prefs ->
        WidgetSettings(
            mode = prefs[Keys.MODE]?.let { runCatching { WidgetMode.valueOf(it) }.getOrNull() } ?: WidgetMode.HEATMAP,
            palette = prefs[Keys.PALETTE]?.let { runCatching { Palette.valueOf(it) }.getOrNull() } ?: Palette.GITHUB,
            surface = prefs[Keys.SURFACE]?.let { runCatching { Surface.valueOf(it) }.getOrNull() } ?: Surface.CARD,
            goal = prefs[Keys.GOAL] ?: 8,
            range = prefs[Keys.RANGE]?.let { d -> Range.entries.find { it.days == d } } ?: Range.FOURTEEN,
            refresh = prefs[Keys.REFRESH]?.let { runCatching { RefreshInterval.valueOf(it) }.getOrNull() } ?: RefreshInterval.ONE_HOUR,
            tapAction = prefs[Keys.TAP_ACTION]?.let { runCatching { TapAction.valueOf(it) }.getOrNull() } ?: TapAction.OPEN_APP,
            weeklyDigest = prefs[Keys.WEEKLY_DIGEST] ?: true,
            digestDay = prefs[Keys.DIGEST_DAY] ?: 1,
            digestHour = prefs[Keys.DIGEST_HOUR] ?: "09:00",
            alertStreak = prefs[Keys.ALERT_STREAK] ?: true,
            alertGoal = prefs[Keys.ALERT_GOAL] ?: false,
            alertMilestones = prefs[Keys.ALERT_MILESTONES] ?: true,
        )
    }

    val historyFlow: Flow<List<DayCommit>> = context.dataStore.data.map { prefs ->
        prefs[Keys.HISTORY_JSON]?.toDayCommitList() ?: emptyList()
    }

    val accountFlow: Flow<GitHubAccount?> = context.dataStore.data.map { prefs ->
        val login = prefs[Keys.ACCOUNT_LOGIN]
        val avatar = prefs[Keys.ACCOUNT_AVATAR]
        if (login != null && avatar != null) GitHubAccount(login, avatar) else null
    }

    val lastErrorFlow: Flow<String?> = context.dataStore.data.map { it[Keys.LAST_ERROR] }

    suspend fun currentSettings(): WidgetSettings = settingsFlow.first()

    suspend fun update(transform: (WidgetSettings) -> WidgetSettings) {
        val next = transform(currentSettings())
        context.dataStore.edit { prefs ->
            prefs[Keys.MODE] = next.mode.name
            prefs[Keys.PALETTE] = next.palette.name
            prefs[Keys.SURFACE] = next.surface.name
            prefs[Keys.GOAL] = next.goal
            prefs[Keys.RANGE] = next.range.days
            prefs[Keys.REFRESH] = next.refresh.name
            prefs[Keys.TAP_ACTION] = next.tapAction.name
            prefs[Keys.WEEKLY_DIGEST] = next.weeklyDigest
            prefs[Keys.DIGEST_DAY] = next.digestDay
            prefs[Keys.DIGEST_HOUR] = next.digestHour
            prefs[Keys.ALERT_STREAK] = next.alertStreak
            prefs[Keys.ALERT_GOAL] = next.alertGoal
            prefs[Keys.ALERT_MILESTONES] = next.alertMilestones
        }
    }

    suspend fun saveHistory(history: List<DayCommit>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HISTORY_JSON] = history.toJson()
            prefs[Keys.LAST_SYNC_EPOCH] = System.currentTimeMillis().toString()
            prefs[Keys.LAST_ERROR] = ""
        }
    }

    suspend fun saveAccount(account: GitHubAccount?) {
        context.dataStore.edit { prefs ->
            if (account == null) {
                prefs.remove(Keys.ACCOUNT_LOGIN)
                prefs.remove(Keys.ACCOUNT_AVATAR)
            } else {
                prefs[Keys.ACCOUNT_LOGIN] = account.login
                prefs[Keys.ACCOUNT_AVATAR] = account.avatarUrl
            }
        }
    }

    suspend fun saveError(message: String) {
        context.dataStore.edit { prefs -> prefs[Keys.LAST_ERROR] = message }
    }

    /** Najdłuższa seria, dla której już wysłano powiadomienie o kamieniu milowym — chroni przed spamem. */
    suspend fun bestStreakSeen(): Int = context.dataStore.data.first()[Keys.BEST_STREAK_SEEN] ?: 0

    suspend fun setBestStreakSeen(value: Int) {
        context.dataStore.edit { prefs -> prefs[Keys.BEST_STREAK_SEEN] = value }
    }

    /** Najlepszy dzienny wynik, dla którego już wysłano powiadomienie o rekordzie. */
    suspend fun bestDaySeen(): Int = context.dataStore.data.first()[Keys.BEST_DAY_SEEN] ?: 0

    suspend fun setBestDaySeen(value: Int) {
        context.dataStore.edit { prefs -> prefs[Keys.BEST_DAY_SEEN] = value }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
