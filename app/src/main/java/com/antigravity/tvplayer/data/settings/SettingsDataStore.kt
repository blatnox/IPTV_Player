package com.antigravity.tvplayer.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.antigravity.tvplayer.domain.model.StreamFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "tv_player_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_STREAM_FORMAT = stringPreferencesKey("preferred_stream_format")
        private val KEY_RESUME_LAST_CHANNEL = booleanPreferencesKey("resume_last_channel")
        private val KEY_LAST_WATCHED_STREAM_ID = intPreferencesKey("last_watched_stream_id")
        private val KEY_CUSTOM_USER_AGENT = stringPreferencesKey("custom_user_agent")
        const val DEFAULT_USER_AGENT = "IPTVSmartersPro/3.1.5 (Linux; Android TV)"
    }

    val preferredStreamFormat: Flow<StreamFormat> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_STREAM_FORMAT]
        StreamFormat.fromExtension(raw)
    }

    val resumeLastChannel: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_RESUME_LAST_CHANNEL] ?: false
    }

    val lastWatchedStreamId: Flow<Int?> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_WATCHED_STREAM_ID]
    }

    val customUserAgent: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CUSTOM_USER_AGENT] ?: DEFAULT_USER_AGENT
    }

    suspend fun setPreferredStreamFormat(format: StreamFormat) {
        context.dataStore.edit { prefs ->
            prefs[KEY_STREAM_FORMAT] = format.extension
        }
    }

    suspend fun setResumeLastChannel(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_RESUME_LAST_CHANNEL] = enabled
        }
    }

    suspend fun setLastWatchedStreamId(streamId: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_WATCHED_STREAM_ID] = streamId
        }
    }

    suspend fun setCustomUserAgent(userAgent: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CUSTOM_USER_AGENT] = userAgent.ifBlank { DEFAULT_USER_AGENT }
        }
    }
}
