package com.mbox.stream.ui.addons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mbox.stream.data.AddOnRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddOnViewModel @Inject constructor(
    private val addOnRepository: AddOnRepository
) : ViewModel() {

    val isYouTubeEnabled: StateFlow<Boolean> = addOnRepository.isYouTubeEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun setYouTubeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            addOnRepository.setYouTubeEnabled(enabled)
        }
    }
}
