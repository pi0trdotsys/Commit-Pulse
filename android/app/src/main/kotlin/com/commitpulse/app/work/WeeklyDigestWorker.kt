package com.commitpulse.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.commitpulse.app.commitPulseApp
import kotlinx.coroutines.flow.first

class WeeklyDigestWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext.commitPulseApp()
        val settings = app.settingsRepository.currentSettings()
        if (!settings.weeklyDigest) return Result.success()

        val history = app.settingsRepository.historyFlow.first()
        if (history.isEmpty()) return Result.success()

        NotificationHelper.ensureChannels(applicationContext)
        NotificationHelper.showWeeklyDigest(applicationContext, history)
        return Result.success()
    }
}
