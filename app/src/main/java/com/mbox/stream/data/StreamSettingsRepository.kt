package com.mbox.stream.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.streamSettingsDataStore by preferencesDataStore(name = "mbox_stream_preferences")

@Singleton
class StreamSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val youtubeEnabledKey = booleanPreferencesKey("youtube_music_enabled")
    private val keepPlayingRadioKey = booleanPreferencesKey("keep_playing_radio")
    private val maxCacheSizeMbKey = longPreferencesKey("max_cache_size_mb")

    val isYouTubeEnabled: Flow<Boolean> = context.streamSettingsDataStore.data.map { prefs ->
        prefs[youtubeEnabledKey] ?: false
    }

    val keepPlayingRadio: Flow<Boolean> = context.streamSettingsDataStore.data.map { prefs ->
        prefs[keepPlayingRadioKey] ?: true
    }

    val maxCacheSizeMb: Flow<Long> = context.streamSettingsDataStore.data.map { prefs ->
        prefs[maxCacheSizeMbKey] ?: 512L
    }

    suspend fun setYouTubeEnabled(enabled: Boolean) {
        context.streamSettingsDataStore.edit { prefs ->
            prefs[youtubeEnabledKey] = enabled
        }
    }

    suspend fun setKeepPlayingRadio(enabled: Boolean) {
        context.streamSettingsDataStore.edit { prefs ->
            prefs[keepPlayingRadioKey] = enabled
        }
    }

    suspend fun setMaxCacheSizeMb(sizeMb: Long) {
        context.streamSettingsDataStore.edit { prefs ->
            prefs[maxCacheSizeMbKey] = sizeMb
        }
    }
}
