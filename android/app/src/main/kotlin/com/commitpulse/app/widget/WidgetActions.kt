package com.commitpulse.app.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.glance.GlanceId
import androidx.glance.appwidget.action.ActionCallback
import com.commitpulse.app.commitPulseApp
import com.commitpulse.app.github.GitHubResult
import kotlinx.coroutines.flow.first

/** Odświeża dane z GitHub i przerysowuje widget — akcja tapnięcia "Odśwież". */
class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: androidx.glance.action.ActionParameters) {
        val app = context.commitPulseApp()
        when (val result = app.gitHubRepository.fetchHistory()) {
            is GitHubResult.Success -> {
                app.settingsRepository.saveAccount(result.value.first)
                app.settingsRepository.saveHistory(result.value.second)
            }
            is GitHubResult.Failure -> {
                app.settingsRepository.saveError(result.message)
            }
        }
        CommitPulseWidget().update(context, glanceId)
    }
}

/** Otwiera profil GitHub w przeglądarce; jeśli konto nie jest jeszcze znane, otwiera aplikację. */
class OpenProfileAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: androidx.glance.action.ActionParameters) {
        val app = context.commitPulseApp()
        val account = app.settingsRepository.accountFlow.first()
        val intent = if (account != null) {
            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/${account.login}"))
        } else {
            context.packageManager.getLaunchIntentForPackage(context.packageName)
        }
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent?.let { context.startActivity(it) }
    }
}
