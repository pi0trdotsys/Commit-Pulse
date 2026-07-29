package com.commitpulse.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.commitpulse.app.commitPulseApp
import com.commitpulse.app.data.today
import kotlinx.coroutines.flow.first

/** Codzienne sprawdzenie o 20:00 — ostrzega, jeśli seria może się urwać. */
class StreakAlertWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext.commitPulseApp()
        val settings = app.settingsRepository.currentSettings()
        if (!settings.alertStreak) return Result.success()

        val history = app.settingsRepository.historyFlow.first()
        if (history.isEmpty() || history.today() > 0) return Result.success()

        NotificationHelper.ensureChannels(applicationContext)
        NotificationHelper.showStreakAtRisk(applicationContext)
        return Result.success()
    }
}
