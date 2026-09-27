package com.commitpulse.app.work

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.commitpulse.app.commitPulseApp
import com.commitpulse.app.data.summarize
import com.commitpulse.app.github.GitHubResult
import com.commitpulse.app.widget.CommitPulseWidget
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** Codzienne sprawdzenie o 20:00 — ostrzega, jeśli aktywna seria może się dziś urwać. */
class StreakAlertWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext.commitPulseApp()
        val settings = app.settingsRepository.currentSettings()
        if (!settings.alertStreak) return Result.success()

        // Świeże dane przed alarmem — ostatnia synchronizacja mogła być sprzed kilku godzin.
        if (app.gitHubRepository.hasToken()) {
            val result = app.gitHubRepository.fetchHistory()
            if (result is GitHubResult.Success) {
                app.settingsRepository.saveAccount(result.value.first)
                app.settingsRepository.saveHistory(result.value.second)
                CommitPulseWidget().updateAll(applicationContext)
            }
        }

        val history = app.settingsRepository.historyFlow.first()
        val summary = summarize(history, LocalDate.now())
        if (summary.todayCount > 0 || summary.streak == 0) return Result.success()

        NotificationHelper.ensureChannels(applicationContext)
        NotificationHelper.showStreakAtRisk(applicationContext)
        return Result.success()
    }
}
