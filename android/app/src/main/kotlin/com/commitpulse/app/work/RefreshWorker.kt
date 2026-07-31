package com.commitpulse.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.glance.appwidget.updateAll
import com.commitpulse.app.commitPulseApp
import com.commitpulse.app.data.DayCommit
import com.commitpulse.app.data.maxCount
import com.commitpulse.app.data.streak
import com.commitpulse.app.data.today
import com.commitpulse.app.github.GitHubResult
import com.commitpulse.app.settings.SettingsRepository
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
                if (settings.alertMilestones) {
                    celebrateMilestones(applicationContext, settingsRepo, history)
                }
                Result.success()
            }
            is GitHubResult.Failure -> {
                settingsRepo.saveError(result.message)
                Result.retry()
            }
        }
    }

    /** Świętuje nowy kamień milowy serii i/lub nowy rekord dnia — co najwyżej raz na przekroczenie. */
    private suspend fun celebrateMilestones(
        context: Context,
        settingsRepo: SettingsRepository,
        history: List<DayCommit>,
    ) {
        val currentStreak = history.streak()
        val bestStreakSeen = settingsRepo.bestStreakSeen()
        val newMilestone = NotificationHelper.STREAK_MILESTONES
            .filter { it in (bestStreakSeen + 1)..currentStreak }
            .maxOrNull()
        if (newMilestone != null) {
            NotificationHelper.ensureChannels(context)
            NotificationHelper.showStreakMilestone(context, newMilestone)
            settingsRepo.setBestStreakSeen(currentStreak)
        }

        val todayCount = history.today()
        val previousBest = history.dropLast(1).maxCount()
        val bestDaySeen = settingsRepo.bestDaySeen()
        if (todayCount > 0 && todayCount > previousBest && todayCount > bestDaySeen) {
            NotificationHelper.ensureChannels(context)
            NotificationHelper.showPersonalBest(context, todayCount)
            settingsRepo.setBestDaySeen(todayCount)
        }
    }
}
