package com.commitpulse.app.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.commitpulse.app.data.DayCommit
import com.commitpulse.app.data.WeekDelta
import com.commitpulse.app.data.lastN
import com.commitpulse.app.data.sum
import com.commitpulse.app.data.weekOverWeek
import com.commitpulse.app.ui.MainActivity

object NotificationHelper {
    const val CHANNEL_DIGEST = "weekly_digest"
    const val CHANNEL_ALERTS = "activity_alerts"

    private const val NOTIF_ID_DIGEST = 1001
    private const val NOTIF_ID_STREAK = 1002
    private const val NOTIF_ID_GOAL = 1003

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_DIGEST, "Podsumowanie tygodnia", NotificationManager.IMPORTANCE_DEFAULT),
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, "Alerty aktywności", NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    private fun contentIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun showWeeklyDigest(context: Context, history: List<DayCommit>) {
        val week = history.lastN(7)
        val wow = week.weekOverWeek()
        val up = wow.direction == WeekDelta.Direction.UP
        val text = "Ubiegły tydzień: ${week.sum()} commitów ${if (up) "▲" else "▼"} ${wow.label} vs poprzedni tydzień"

        val notification = NotificationCompat.Builder(context, CHANNEL_DIGEST)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Commit Pulse — podsumowanie tygodnia")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent(context))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notifyIfPermitted(NOTIF_ID_DIGEST, notification)
    }

    fun showStreakAtRisk(context: Context) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Seria zagrożona 🔥")
            .setContentText("Jeszcze nie było dziś commita — nie przerywaj serii.")
            .setContentIntent(contentIntent(context))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notifyIfPermitted(NOTIF_ID_STREAK, notification)
    }

    fun showGoalReached(context: Context, goal: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Cel dzienny osiągnięty 🎯")
            .setContentText("Masz już $goal commitów dzisiaj. Świetna robota!")
            .setContentIntent(contentIntent(context))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notifyIfPermitted(NOTIF_ID_GOAL, notification)
    }

    private fun NotificationManagerCompat.notifyIfPermitted(id: Int, notification: android.app.Notification) {
        if (areNotificationsEnabled()) {
            try {
                notify(id, notification)
            } catch (_: SecurityException) {
                // Brak uprawnienia POST_NOTIFICATIONS — po prostu pomijamy powiadomienie.
            }
        }
    }
}
