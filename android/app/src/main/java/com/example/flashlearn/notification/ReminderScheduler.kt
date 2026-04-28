package com.example.flashlearn.notification

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.flashlearn.sync.ReminderWorker
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    private const val WORK_NAME = "study_reminder"

    fun schedule(context: Context, prefs: SharedPreferences) {
        val hour = prefs.getInt(ReminderWorker.PREF_NOTIFICATION_HOUR, 18)
        val minute = prefs.getInt(ReminderWorker.PREF_NOTIFICATION_MINUTE, 0)

        val now = LocalDateTime.now()
        var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)

        // jeśli godzina już minęła dziś — zaplanuj na jutro
        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }

        val delaySeconds = ChronoUnit.SECONDS.between(now, target)
        Log.d("ReminderScheduler", "Scheduling reminder in ${delaySeconds}s (at $target)")

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        Log.d("ReminderScheduler", "Reminder cancelled")
    }
}