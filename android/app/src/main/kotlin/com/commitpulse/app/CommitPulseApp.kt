package com.commitpulse.app

import android.app.Application
import com.commitpulse.app.github.GitHubRepository
import com.commitpulse.app.settings.SettingsRepository
import com.commitpulse.app.work.NotificationHelper
import com.commitpulse.app.work.WorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CommitPulseApp : Application() {
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val gitHubRepository: GitHubRepository by lazy { GitHubRepository(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)
        appScope.launch {
            WorkScheduler.scheduleAll(applicationContext, settingsRepository.currentSettings())
        }
    }
}

fun android.content.Context.commitPulseApp(): CommitPulseApp =
    applicationContext as CommitPulseApp
