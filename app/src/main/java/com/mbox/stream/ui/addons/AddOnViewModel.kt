package com.mbox.stream.ui.addons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mbox.stream.data.StreamSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddOnViewModel @Inject constructor(
    private val streamSettingsRepository: StreamSettingsRepository
) : ViewModel() {

    val isYouTubeEnabled: StateFlow<Boolean> = streamSettingsRepository.isYouTubeEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val keepPlayingRadio: StateFlow<Boolean> = streamSettingsRepository.keepPlayingRadio.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val maxCacheSizeMb: StateFlow<Long> = streamSettingsRepository.maxCacheSizeMb.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 512L
    )

    fun setYouTubeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            streamSettingsRepository.setYouTubeEnabled(enabled)
        }
    }

    fun setKeepPlayingRadio(enabled: Boolean) {
        viewModelScope.launch {
            streamSettingsRepository.setKeepPlayingRadio(enabled)
        }
    }

    fun setMaxCacheSizeMb(sizeMb: Long) {
        viewModelScope.launch {
            streamSettingsRepository.setMaxCacheSizeMb(sizeMb)
        }
    }
}
