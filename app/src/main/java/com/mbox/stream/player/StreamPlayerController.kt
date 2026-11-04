package com.mbox.stream.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.mediaplayer.data.model.*
import com.mbox.stream.queue.QueueManager
import com.mbox.stream.service.StreamMediaSessionService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(UnstableApi::class)
class StreamPlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    val queueManager: QueueManager
) : PlayerController, CastController {

    companion object {
        private const val TAG = "MBoxStream"
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var mediaController: MediaController? = null

    private val _nowPlayingState = MutableStateFlow(NowPlayingState())
    val nowPlayingState: StateFlow<NowPlayingState> = _nowPlayingState.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    private var activeQualityLabel = ""
    private var currentSourceTrack: SourceTrack? = null
    private var currentResolvedStream: ResolvedStream? = null

    init {
        connectToService()
    }

    private fun connectToService() {
        try {
            val sessionToken = SessionToken(
                context,
                ComponentName(context, StreamMediaSessionService::class.java)
            )
            val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
            controllerFuture.addListener({
                try {
                    val controller = controllerFuture.get()
                    mediaController = controller
                    Log.i(TAG, "MediaController connected to StreamMediaSessionService successfully")
                    attachControllerListeners(controller)
                } catch (e: Exception) {
                    Log.e(TAG, "MediaController connection error: ${e.message}", e)
                }
            }, MoreExecutors.directExecutor())
        } catch (e: Exception) {
            Log.e(TAG, "SessionToken error: ${e.message}", e)
        }
    }

    private fun attachControllerListeners(controller: MediaController) {
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.i(TAG, "Player.Listener onIsPlayingChanged: $isPlaying")
                _nowPlayingState.value = _nowPlayingState.value.copy(isPlaying = isPlaying)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val stateStr = when (playbackState) {
                    Player.STATE_IDLE -> "STATE_IDLE (1)"
                    Player.STATE_BUFFERING -> "STATE_BUFFERING (2)"
                    Player.STATE_READY -> "STATE_READY (3)"
                    Player.STATE_ENDED -> "STATE_ENDED (4)"
                    else -> "UNKNOWN ($playbackState)"
                }
                Log.i(TAG, "Player.Listener onPlaybackStateChanged: $stateStr")

                when (playbackState) {
                    Player.STATE_ENDED -> {
                        val nextItem = queueManager.next()
                        if (nextItem != null) {
                            playSourceTrack(
                                nextItem.sourceTrack,
                                ResolvedStream(
                                    url = "mbox://yt/${nextItem.sourceTrack.id}",
                                    codec = "Opus",
                                    bitrateKbps = 160,
                                    sampleRateHz = 48000,
                                    container = "webm",
                                    lossy = true,
                                    expiresAtMs = System.currentTimeMillis() + 14400000L
                                )
                            )
                        } else {
                            _nowPlayingState.value = _nowPlayingState.value.copy(isPlaying = false)
                        }
                    }
                    Player.STATE_READY -> {
                        _playbackError.value = null
                    }
                    else -> {}
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e(TAG, "Player.Listener onPlayerError: ${error.errorCodeName} - ${error.message}", error)
                _playbackError.value = "This song can't play right now"
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                Log.i(TAG, "onMediaItemTransition: title=${mediaItem?.mediaMetadata?.title}")
                if (mediaItem != null) {
                    updateStateFromMediaItem(mediaItem)
                }
            }
        })

        // Position tracking ticker
        scope.launch {
            while (isActive) {
                val mc = mediaController
                if (mc != null && mc.isPlaying) {
                    val pos = mc.currentPosition.coerceAtLeast(0L)
                    val dur = mc.duration.coerceAtLeast(0L)
                    _nowPlayingState.value = _nowPlayingState.value.copy(
                        positionMs = pos,
                        durationMs = dur,
                        vinylRotation = (_nowPlayingState.value.vinylRotation + 2f) % 360f
                    )
                }
                delay(100)
            }
        }
    }

    private fun updateStateFromMediaItem(mediaItem: MediaItem) {
        val metadata = mediaItem.mediaMetadata
        val title = metadata.title?.toString() ?: "Unknown Title"
        val artist = metadata.artist?.toString() ?: "Unknown Artist"
        val artworkUri = metadata.artworkUri?.toString()
        val qualityLabel = metadata.extras?.getString("qualityLabel") ?: activeQualityLabel
        val codec = metadata.extras?.getString("codec") ?: "OPUS"
        val bitrate = metadata.extras?.getInt("bitrateKbps") ?: 160

        val track = Track(
            id = (mediaItem.mediaId.hashCode().toLong() and 0x7FFFFFFFFFFFFFFF),
            uri = mediaItem.localConfiguration?.uri?.toString() ?: "",
            title = title,
            artist = artist,
            album = "YouTube Music",
            albumArtUri = artworkUri,
            duration = _nowPlayingState.value.durationMs,
            bitrate = bitrate * 1000,
            sampleRate = 48000,
            trackNumber = 1,
            discNumber = 1,
            year = 2026,
            genre = "Stream",
            composer = artist,
            comment = "",
            fileSize = 0L,
            dateAdded = System.currentTimeMillis(),
            mimeType = "audio/webm",
            folderName = "YouTube Music",
            folderPath = "YouTube Music"
        )

        _nowPlayingState.value = _nowPlayingState.value.copy(
            currentTrack = track,
            currentAudioSpec = qualityLabel,
            signalPath = AudioSignalPath(
                format = codec.uppercase(),
                sourceInfo = "$bitrate kbps",
                decoder = "ExoPlayer (${codec.uppercase()})",
                hardwareInfo = "48.0 kHz",
                outputApi = "AudioTrack",
                latency = 0,
                bitDepth = "16-bit PCM",
                deviceType = "This phone"
            )
        )
    }

    fun playSourceTrack(sourceTrack: SourceTrack, resolvedStream: ResolvedStream) {
        Log.i(TAG, "playSourceTrack() called for track: '${sourceTrack.title}'")
        currentSourceTrack = sourceTrack
        currentResolvedStream = resolvedStream
        activeQualityLabel = resolvedStream.toQualityLabel("YouTube Music")
        _playbackError.value = null

        val track = Track(
            id = (sourceTrack.id.hashCode().toLong() and 0x7FFFFFFFFFFFFFFF),
            uri = resolvedStream.url,
            title = sourceTrack.title,
            artist = sourceTrack.artist,
            album = sourceTrack.album ?: "YouTube Music",
            albumArtUri = sourceTrack.artworkUrl,
            duration = sourceTrack.durationMs,
            bitrate = resolvedStream.bitrateKbps * 1000,
            sampleRate = resolvedStream.sampleRateHz ?: 48000,
            trackNumber = 1,
            discNumber = 1,
            year = 2026,
            genre = "Stream",
            composer = sourceTrack.artist,
            comment = "",
            fileSize = 0L,
            dateAdded = System.currentTimeMillis(),
            mimeType = "audio/${resolvedStream.container}",
            folderName = "YouTube Music",
            folderPath = "YouTube Music"
        )

        _nowPlayingState.value = _nowPlayingState.value.copy(
            currentTrack = track,
            isPlaying = true,
            positionMs = 0L,
            durationMs = sourceTrack.durationMs,
            currentAudioSpec = activeQualityLabel,
            signalPath = AudioSignalPath(
                format = resolvedStream.codec.uppercase(),
                sourceInfo = "${resolvedStream.bitrateKbps} kbps",
                decoder = "ExoPlayer (${resolvedStream.codec.uppercase()})",
                hardwareInfo = "48.0 kHz",
                outputApi = "AudioTrack",
                latency = 0,
                bitDepth = "16-bit PCM",
                deviceType = "This phone"
            )
        )

        val extras = Bundle().apply {
            putString("qualityLabel", activeQualityLabel)
            putString("codec", resolvedStream.codec)
            putInt("bitrateKbps", resolvedStream.bitrateKbps)
            putBoolean("lossy", resolvedStream.lossy)
            putLong("durationMs", sourceTrack.durationMs)
        }

        val metadata = MediaMetadata.Builder()
            .setTitle(sourceTrack.title)
            .setArtist(sourceTrack.artist)
            .setAlbumTitle("YouTube Music")
            .setArtworkUri(sourceTrack.artworkUrl?.let { Uri.parse(it) })
            .setExtras(extras)
            .build()

        val mediaItem = MediaItem.Builder()
            .setMediaId(sourceTrack.id)
            .setUri(Uri.parse(resolvedStream.url))
            .setMimeType("audio/${resolvedStream.container}")
            .setMediaMetadata(metadata)
            .build()

        val mc = mediaController
        if (mc != null) {
            Log.i(TAG, "MediaController setMediaItem & prepare for '${sourceTrack.title}'")
            mc.setMediaItem(mediaItem)
            mc.prepare()
            mc.play()
        } else {
            Log.w(TAG, "MediaController not yet connected! Reconnecting...")
            connectToService()
        }
    }

    fun retryPlayback() {
        Log.i(TAG, "retryPlayback() called")
        val stream = currentResolvedStream
        val track = currentSourceTrack
        if (stream != null && track != null) {
            playSourceTrack(track, stream)
        }
    }

    fun clearError() {
        _playbackError.value = null
    }

    override fun togglePlayPause() {
        val mc = mediaController ?: return
        Log.i(TAG, "togglePlayPause() called, isPlaying=${mc.isPlaying}")
        if (mc.isPlaying) {
            mc.pause()
        } else {
            mc.play()
        }
        _nowPlayingState.value = _nowPlayingState.value.copy(isPlaying = mc.isPlaying)
    }

    override fun play() {
        Log.i(TAG, "play() called")
        mediaController?.play()
        _nowPlayingState.value = _nowPlayingState.value.copy(isPlaying = true)
    }

    override fun pause() {
        Log.i(TAG, "pause() called")
        mediaController?.pause()
        _nowPlayingState.value = _nowPlayingState.value.copy(isPlaying = false)
    }

    override fun next() {
        val nextItem = queueManager.next()
        if (nextItem != null) {
            playSourceTrack(
                nextItem.sourceTrack,
                ResolvedStream(
                    url = "mbox://yt/${nextItem.sourceTrack.id}",
                    codec = "Opus",
                    bitrateKbps = 160,
                    sampleRateHz = 48000,
                    container = "webm",
                    lossy = true,
                    expiresAtMs = System.currentTimeMillis() + 14400000L
                )
            )
        } else {
            mediaController?.seekToNext()
        }
    }

    override fun previous() {
        val prevItem = queueManager.previous()
        if (prevItem != null) {
            playSourceTrack(
                prevItem.sourceTrack,
                ResolvedStream(
                    url = "mbox://yt/${prevItem.sourceTrack.id}",
                    codec = "Opus",
                    bitrateKbps = 160,
                    sampleRateHz = 48000,
                    container = "webm",
                    lossy = true,
                    expiresAtMs = System.currentTimeMillis() + 14400000L
                )
            )
        } else {
            mediaController?.seekToPrevious()
        }
    }

    override fun seekTo(positionMs: Long) {
        Log.i(TAG, "seekTo($positionMs) called")
        mediaController?.seekTo(positionMs)
        _nowPlayingState.value = _nowPlayingState.value.copy(positionMs = positionMs)
    }

    override fun seekLive(targetMs: Long, source: String) {
        _nowPlayingState.value = _nowPlayingState.value.copy(positionMs = targetMs)
    }

    override fun seekFinal(targetMs: Long, resume: Boolean, source: String) {
        Log.i(TAG, "seekFinal($targetMs, resume=$resume) called")
        val mc = mediaController ?: return
        mc.seekTo(targetMs)
        if (resume && !mc.isPlaying) mc.play()
    }

    override fun setVisualizerMode(modeId: String) {
        _nowPlayingState.value = _nowPlayingState.value.copy(activeVisualizerMode = modeId)
    }

    override fun pauseForDjHold() { mediaController?.pause() }
    override fun resumeAfterDjHold() { mediaController?.play() }
    override fun skipToQueueItem(index: Int) {}
    override fun updateLyricsManually(trackId: Long, text: String) {}
    override fun playTracks(
        tracks: List<Track>,
        startIndex: Int,
        sourceType: String,
        sourceName: String,
        sourcePath: String
    ) {}
    override fun expandPlayer() {}
    override fun collapsePlayer() {}

    // CastController
    override fun sendSetLyricsToTv(enabled: Boolean) {}
    override fun sendSetDisplayModeToTv(mode: String) {}
    fun selectOutputDevice(device: String) {}
}
