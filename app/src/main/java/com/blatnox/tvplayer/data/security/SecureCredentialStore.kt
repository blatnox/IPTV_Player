package com.blatnox.tvplayer.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureCredentialStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "tvplayer_secure_creds",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback for emulators/environments with Keystore initialization quirks
            context.getSharedPreferences("tvplayer_secure_creds_fallback", Context.MODE_PRIVATE)
        }
    }

    fun savePassword(accountKey: String, password: String) {
        prefs.edit().putString(accountKey, password).apply()
    }

    fun getPassword(accountKey: String): String? {
        return prefs.getString(accountKey, null)
    }

    fun deletePassword(accountKey: String) {
        prefs.edit().remove(accountKey).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
