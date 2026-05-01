package com.example.flashlearn.sync

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.flashlearn.notification.NotificationHelper
import com.flashlearn.data.dao.FlashcardProgressDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val flashcardProgressDao: FlashcardProgressDao,
    private val prefs: SharedPreferences
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "ReminderWorker started")

        // sprawdź czy powiadomienia są włączone
        val enabled = prefs.getBoolean(PREF_NOTIFICATIONS_ENABLED, true)
        if (!enabled) {
            Log.d(TAG, "Notifications disabled — skipping")
            return Result.success()
        }

        // sprawdź czy user uczył się dziś (last_review_date w FlashcardProgress)
        val today = LocalDate.now().toEpochDay()
        val studiedToday = flashcardProgressDao.hasStudySessionToday(today)

        if (!studiedToday) {
            Log.d(TAG, "User has not studied today — showing notification")
            NotificationHelper.showStudyReminder(applicationContext)
        } else {
            Log.d(TAG, "User already studied today — skipping")
        }

        return Result.success()
    }

    companion object {
        const val TAG = "ReminderWorker"
        const val PREF_NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val PREF_NOTIFICATION_HOUR = "notification_hour"
        const val PREF_NOTIFICATION_MINUTE = "notification_minute"
    }
}