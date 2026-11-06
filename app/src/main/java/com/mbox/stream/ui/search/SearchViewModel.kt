package com.mbox.stream.ui.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediaplayer.data.model.MusicSource
import com.mediaplayer.data.model.SourceTrack
import com.mbox.stream.data.StreamSettingsRepository
import com.mbox.stream.player.StreamPlayerController
import com.mbox.stream.queue.QueueManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

sealed interface SearchUiState {
    object Disabled : SearchUiState
    object Initial : SearchUiState
    object Loading : SearchUiState
    data class Success(val tracks: List<SourceTrack>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val musicSources: Set<@JvmSuppressWildcards MusicSource>,
    private val streamSettingsRepository: StreamSettingsRepository,
    val playerController: StreamPlayerController,
    val queueManager: QueueManager
) : ViewModel() {

    val isYouTubeEnabled = streamSettingsRepository.isYouTubeEnabled
        .onEach { Log.i("MBoxStream", "youtubeMusicEnabled=$it") }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Initial)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _isResolving = MutableStateFlow(false)
    val isResolving: StateFlow<Boolean> = _isResolving.asStateFlow()

    private val _resolvingTrackId = MutableStateFlow<String?>(null)
    val resolvingTrackId: StateFlow<String?> = _resolvingTrackId.asStateFlow()

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    private var searchJob: Job? = null
    private var resolveJob: Job? = null
    private var lastSelectedTrack: SourceTrack? = null

    fun onQueryChanged(query: String) {
        searchJob?.cancel()

        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _uiState.value = SearchUiState.Initial
            return
        }

        searchJob = viewModelScope.launch {
            delay(400)
            _uiState.value = SearchUiState.Loading
            try {
                val source = musicSources.firstOrNull { it.id == "youtube" }
                if (source == null) {
                    _uiState.value = SearchUiState.Error("YouTube Music source not available")
                    return@launch
                }
                val results = source.search(trimmed)
                if (results.isEmpty()) {
                    _uiState.value = SearchUiState.Error("No songs found")
                } else {
                    _uiState.value = SearchUiState.Success(results)
                }
            } catch (e: IOException) {
                _uiState.value = SearchUiState.Error("No internet connection")
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error("No songs found")
            }
        }
    }

    fun playTrack(sourceTrack: SourceTrack) {
        if (_resolvingTrackId.value == sourceTrack.id) {
            return
        }

        resolveJob?.cancel()
        lastSelectedTrack = sourceTrack
        queueManager.playNow(sourceTrack)

        _resolvingTrackId.value = sourceTrack.id
        _isResolving.value = true

        resolveJob = viewModelScope.launch {
            try {
                val source = musicSources.firstOrNull { it.id == sourceTrack.sourceId }
                    ?: musicSources.firstOrNull { it.id == "youtube" }
                if (source == null) {
                    _snackbarEvent.emit("This song can't play right now")
                    return@launch
                }
                val resolvedStream = source.resolve(sourceTrack)
                playerController.playSourceTrack(sourceTrack, resolvedStream)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _snackbarEvent.emit("No internet connection")
            } catch (e: Exception) {
                _snackbarEvent.emit("This song can't play right now")
            } finally {
                if (_resolvingTrackId.value == sourceTrack.id) {
                    _resolvingTrackId.value = null
                    _isResolving.value = false
                }
            }
        }
    }

    fun playNext(sourceTrack: SourceTrack) {
        queueManager.playNext(sourceTrack)
        viewModelScope.launch {
            _snackbarEvent.emit("Added '${sourceTrack.title}' to Play Next")
        }
    }

    fun addToQueue(sourceTrack: SourceTrack) {
        queueManager.addToQueue(sourceTrack)
        viewModelScope.launch {
            _snackbarEvent.emit("Added '${sourceTrack.title}' to Queue")
        }
    }

    fun retryLastTrack() {
        val track = lastSelectedTrack
        if (track != null) {
            playTrack(track)
        }
    }
}
