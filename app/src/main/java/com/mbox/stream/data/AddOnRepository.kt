package com.mbox.stream.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "mbox_stream_settings")

@Singleton
class AddOnRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val youtubeEnabledKey = booleanPreferencesKey("youtube_music_enabled")

    val isYouTubeEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[youtubeEnabledKey] ?: false
    }

    suspend fun setYouTubeEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[youtubeEnabledKey] = enabled
        }
    }
}
