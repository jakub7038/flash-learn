package com.example.flashlearn.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object TokenManager {
    private const val PREFS_NAME = "flashlearn_auth"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_EMAIL = "email"
    private const val KEY_REGISTERED_AT = "registered_at"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun saveEmail(email: String) {
        prefs.edit().putString(KEY_EMAIL, email).apply()
    }

    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)

    fun saveRegisteredAt(isoDate: String) {
        prefs.edit().putString(KEY_REGISTERED_AT, isoDate).apply()
    }

    fun getRegisteredAt(): String? = prefs.getString(KEY_REGISTERED_AT, null)

    fun clearTokens() {
        prefs.edit().clear().apply()
    }

    fun migrateFrom(legacyPrefs: SharedPreferences) {
        val legacyAccessToken = legacyPrefs.getString(KEY_ACCESS_TOKEN, null)
        val legacyRefreshToken = legacyPrefs.getString(KEY_REFRESH_TOKEN, null)
        if (getAccessToken() == null && legacyAccessToken != null && legacyRefreshToken != null) {
            saveTokens(legacyAccessToken, legacyRefreshToken)
            legacyPrefs.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .apply()
        }

        if (getEmail() == null) {
            legacyPrefs.getString(KEY_EMAIL, null)?.let(::saveEmail)
        }

        if (getRegisteredAt() == null) {
            legacyPrefs.getString(KEY_REGISTERED_AT, null)?.let(::saveRegisteredAt)
        }
    }

    fun isLoggedIn(): Boolean = getAccessToken() != null
}
