package com.commitpulse.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.glance.appwidget.updateAll
import com.commitpulse.app.commitPulseApp
import com.commitpulse.app.data.today
import com.commitpulse.app.github.GitHubResult
import com.commitpulse.app.widget.CommitPulseWidget

class RefreshWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext.commitPulseApp()
        val settingsRepo = app.settingsRepository

        if (!app.gitHubRepository.hasToken()) return Result.success()

        return when (val result = app.gitHubRepository.fetchHistory()) {
            is GitHubResult.Success -> {
                val (account, history) = result.value
                settingsRepo.saveAccount(account)
                settingsRepo.saveHistory(history)
                CommitPulseWidget().updateAll(applicationContext)

                val settings = settingsRepo.currentSettings()
                if (settings.alertGoal && history.today() >= settings.goal && settings.goal > 0) {
                    NotificationHelper.ensureChannels(applicationContext)
                    NotificationHelper.showGoalReached(applicationContext, settings.goal)
                }
                Result.success()
            }
            is GitHubResult.Failure -> {
                settingsRepo.saveError(result.message)
                Result.retry()
            }
        }
    }
}
