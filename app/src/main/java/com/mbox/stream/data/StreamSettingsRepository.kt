package com.mbox.stream.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.streamSettingsDataStore by preferencesDataStore(name = "mbox_stream_preferences")
private val Context.legacyAddOnDataStore by preferencesDataStore(name = "mbox_stream_settings")

@Singleton
class StreamSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "MBoxStream"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val youtubeEnabledKey = booleanPreferencesKey("youtube_music_enabled")
    private val keepPlayingRadioKey = booleanPreferencesKey("keep_playing_radio")
    private val maxCacheSizeMbKey = longPreferencesKey("max_cache_size_mb")
    private val visualizerModeKey = stringPreferencesKey("visualizer_mode")

    val isYouTubeEnabled: Flow<Boolean> = context.streamSettingsDataStore.data.map { prefs ->
        prefs[youtubeEnabledKey] ?: false
    }

    val keepPlayingRadio: Flow<Boolean> = context.streamSettingsDataStore.data.map { prefs ->
        prefs[keepPlayingRadioKey] ?: true
    }

    val maxCacheSizeMb: Flow<Long> = context.streamSettingsDataStore.data.map { prefs ->
        prefs[maxCacheSizeMbKey] ?: 512L
    }

    val visualizerMode: Flow<String> = context.streamSettingsDataStore.data.map { prefs ->
        prefs[visualizerModeKey] ?: "circular_rings"
    }

    init {
        // Migrate legacy setting once if present
        scope.launch {
            try {
                val legacyPrefs = context.legacyAddOnDataStore.data.first()
                val legacyEnabled = legacyPrefs[youtubeEnabledKey]
                if (legacyEnabled != null) {
                    setYouTubeEnabled(legacyEnabled)
                    context.legacyAddOnDataStore.edit { it.clear() }
                }
            } catch (_: Exception) {
                // Ignore migration errors
            }
        }
    }

    suspend fun setYouTubeEnabled(enabled: Boolean) {
        Log.i(TAG, "youtubeMusicEnabled=$enabled")
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

    suspend fun setVisualizerMode(mode: String) {
        context.streamSettingsDataStore.edit { prefs ->
            prefs[visualizerModeKey] = mode
        }
    }
}
