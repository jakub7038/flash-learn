package com.example.flashlearn.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import com.example.flashlearn.notification.ReminderScheduler
import com.example.flashlearn.sync.ReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class SettingsUiState(
    val isNotificationsEnabled: Boolean = false,
    val notificationHour: Int = 18,
    val notificationMinute: Int = 0,
    val language: String = "pl",
    val theme: String = "system"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: SharedPreferences,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    // Tylko jeden, połączony blok init!
    init {
        // 1. Najpierw pobieramy zapisane wartości z SharedPreferences
        val savedLang = prefs.getString("language", "pl") ?: "pl"
        val savedTheme = prefs.getString("theme", "system") ?: "system"

        // 2. Inicjalizujemy stan UI pobranymi danymi
        _uiState.value = SettingsUiState(
            isNotificationsEnabled = prefs.getBoolean("notifications_enabled", false),
            notificationHour = prefs.getInt(ReminderWorker.PREF_NOTIFICATION_HOUR, 18),
            notificationMinute = prefs.getInt(ReminderWorker.PREF_NOTIFICATION_MINUTE, 0),
            language = savedLang,
            theme = savedTheme
        )

        // 3. Wymuszamy załadowanie języka, jeśli systemowy różni się od zapisanego
        val appLocales = LocaleListCompat.forLanguageTags(savedLang)
        if (AppCompatDelegate.getApplicationLocales() != appLocales) {
            AppCompatDelegate.setApplicationLocales(appLocales)
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
        _uiState.update { it.copy(isNotificationsEnabled = enabled) }

        if (enabled) {
            ReminderScheduler.schedule(context, prefs)
        } else {
            ReminderScheduler.cancel(context)
        }
    }

    fun updateNotificationTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(ReminderWorker.PREF_NOTIFICATION_HOUR, hour)
            .putInt(ReminderWorker.PREF_NOTIFICATION_MINUTE, minute)
            .apply()

        _uiState.update { it.copy(notificationHour = hour, notificationMinute = minute) }

        if (_uiState.value.isNotificationsEnabled) {
            ReminderScheduler.schedule(context, prefs)
        }
    }

    fun updateLanguage(lang: String) {
        prefs.edit().putString("language", lang).apply()
        _uiState.update { it.copy(language = lang) }

        val appLocales = LocaleListCompat.forLanguageTags(lang)
        AppCompatDelegate.setApplicationLocales(appLocales)
    }

    fun updateTheme(theme: String) {
        prefs.edit().putString("theme", theme).apply()
        _uiState.update { it.copy(theme = theme) }

        val mode = when (theme) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}