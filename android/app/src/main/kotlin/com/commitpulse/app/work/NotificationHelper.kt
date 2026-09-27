package com.commitpulse.app.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.commitpulse.app.data.DayCommit
import com.commitpulse.app.data.lastCompleteWeek
import com.commitpulse.app.data.weeklyDigestText
import com.commitpulse.app.ui.MainActivity
import java.time.LocalDate

object NotificationHelper {
    const val CHANNEL_DIGEST = "weekly_digest"
    const val CHANNEL_ALERTS = "activity_alerts"

    private const val NOTIF_ID_DIGEST = 1001
    private const val NOTIF_ID_STREAK = 1002
    private const val NOTIF_ID_GOAL = 1003
    private const val NOTIF_ID_MILESTONE = 1004
    private const val NOTIF_ID_PERSONAL_BEST = 1005

    /** Kamienie milowe serii, dla których warto pogratulować. */
    val STREAK_MILESTONES = listOf(3, 7, 14, 21, 30, 50, 75, 100, 150, 200, 365)

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
        val digest = weeklyDigestText(lastCompleteWeek(history, LocalDate.now()))
        val text = "${digest.headline}\n${digest.comparison}"

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
            .setContentText("Dziś jeszcze brak kontrybucji — nie przerywaj serii.")
            .setContentIntent(contentIntent(context))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notifyIfPermitted(NOTIF_ID_STREAK, notification)
    }

    /** Motywujące powiadomienie przy przekroczeniu kolejnego kamienia milowego serii commitów. */
    fun showStreakMilestone(context: Context, streakDays: Int) {
        val body = when {
            streakDays < 14 -> "Rozkręcasz się! $streakDays dni z rzędu — złap tempo i jedź dalej."
            streakDays < 50 -> "$streakDays dni bez przerwy. To już nawyk, nie przypadek — świetna robota."
            else -> "$streakDays dni serii! Niewielu tu dociera — to naprawdę imponujący wynik."
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Seria $streakDays dni! 🔥")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent(context))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notifyIfPermitted(NOTIF_ID_MILESTONE, notification)
    }

    /** Motywujące powiadomienie, gdy dzisiejsza liczba commitów pobije dotychczasowy rekord. */
    fun showPersonalBest(context: Context, count: Int) {
        val text = "Nowy rekord dnia: $count commitów. Najlepszy wynik w historii tego widgetu!"
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Nowy rekord dnia 🚀")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent(context))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notifyIfPermitted(NOTIF_ID_PERSONAL_BEST, notification)
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
