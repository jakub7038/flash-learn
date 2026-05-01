package com.example.flashlearn

import android.app.Application
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.flashlearn.data.remote.RetrofitClient
import com.example.flashlearn.notification.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FlashLearnApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // 1. Wstrzykujemy dokładnie te same SharedPreferences, których używa ekran ustawień!
    @Inject
    lateinit var prefs: SharedPreferences

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // super.onCreate() inicjalizuje Hilta, więc od tego momentu zmienna 'prefs' jest gotowa do użycia

        RetrofitClient.init(this)
        NotificationHelper.createNotificationChannel(this)

        // 2. Odczytujemy zapisany język (domyślnie "pl")
        val savedLang = prefs.getString("language", "pl") ?: "pl"

        // 3. Wymuszamy natychmiastowe załadowanie odpowiedniego języka dla całej aplikacji
        val appLocales = LocaleListCompat.forLanguageTags(savedLang)
        AppCompatDelegate.setApplicationLocales(appLocales)
    }
}