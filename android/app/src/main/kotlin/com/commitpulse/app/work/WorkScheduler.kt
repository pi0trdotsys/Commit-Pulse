package com.commitpulse.app.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.commitpulse.app.data.WidgetSettings
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** (Re)planuje okresowe zadania WorkManager na podstawie aktualnych ustawień. */
object WorkScheduler {
    private const val WORK_REFRESH = "commit_pulse_refresh"
    private const val WORK_WEEKLY_DIGEST = "commit_pulse_weekly_digest"
    private const val WORK_STREAK_ALERT = "commit_pulse_streak_alert"

    fun scheduleAll(context: Context, settings: WidgetSettings) {
        scheduleRefresh(context, settings)
        scheduleWeeklyDigest(context, settings)
        scheduleStreakAlert(context, settings)
    }

    fun scheduleRefresh(context: Context, settings: WidgetSettings) {
        val request = PeriodicWorkRequestBuilder<RefreshWorker>(
            settings.refresh.minutes.coerceAtLeast(15), TimeUnit.MINUTES,
        ).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_REFRESH, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun scheduleWeeklyDigest(context: Context, settings: WidgetSettings) {
        val manager = WorkManager.getInstance(context)
        if (!settings.weeklyDigest) {
            manager.cancelUniqueWork(WORK_WEEKLY_DIGEST)
            return
        }
        val delay = initialDelayMillis(targetIsoDay = settings.digestDay, targetHour = settings.digestHour)
        val request = PeriodicWorkRequestBuilder<WeeklyDigestWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        manager.enqueueUniquePeriodicWork(WORK_WEEKLY_DIGEST, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun scheduleStreakAlert(context: Context, settings: WidgetSettings) {
        val manager = WorkManager.getInstance(context)
        if (!settings.alertStreak) {
            manager.cancelUniqueWork(WORK_STREAK_ALERT)
            return
        }
        val delay = initialDelayMillis(targetIsoDay = null, targetHour = "20:00")
        val request = PeriodicWorkRequestBuilder<StreakAlertWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        manager.enqueueUniquePeriodicWork(WORK_STREAK_ALERT, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    /** Wylicza opóźnienie do najbliższego wystąpienia [targetHour] (HH:mm), opcjonalnie w dzień ISO 1..7 (Pn..Nd). */
    private fun initialDelayMillis(targetIsoDay: Int?, targetHour: String): Long {
        val (h, m) = targetHour.split(":").map { it.toIntOrNull() ?: 0 }
        val now = LocalDateTime.now()
        var target = now.toLocalDate().atTime(LocalTime.of(h, m))
        if (targetIsoDay != null) {
            while (target.dayOfWeek.value != targetIsoDay || !target.isAfter(now)) {
                target = target.plusDays(1)
            }
        } else if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }
        return Duration.between(now, target).toMillis().coerceAtLeast(0)
    }
}
