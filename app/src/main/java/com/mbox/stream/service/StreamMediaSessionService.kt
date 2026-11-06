package com.mbox.stream.service

import android.app.ForegroundServiceStartNotAllowedException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.mediaplayer.data.model.SourceTrack
import com.mediaplayer.service.EqualizerManager
import com.mediaplayer.service.PlayerEngineFactory
import com.mediaplayer.service.PlayerEngineResult
import com.mediaplayer.util.VisualizerManager
import com.mbox.source.youtube.YouTubeMusicSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
@OptIn(UnstableApi::class)
class StreamMediaSessionService : MediaSessionService() {

    companion object {
        private const val TAG = "MBoxStream"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    }

    @Inject
    lateinit var youtubeMusicSource: YouTubeMusicSource

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val eqManager = EqualizerManager()
    val visualizerManager = VisualizerManager()

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "StreamMediaSessionService onCreate() - session/service created")

        try {
            val notificationProvider = DefaultMediaNotificationProvider(this)
            setMediaNotificationProvider(notificationProvider)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set MediaNotificationProvider: ${e.message}", e)
        }

        if (Build.VERSION.SDK_INT >= 31) {
            setListener(object : Listener {
                override fun onForegroundServiceStartNotAllowedException() {
                    Log.e(TAG, "onForegroundServiceStartNotAllowedException caught")
                }
            })
        }

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(USER_AGENT)
            .setAllowCrossProtocolRedirects(true)

        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(httpDataSourceFactory)

        val engineResult: PlayerEngineResult = PlayerEngineFactory.createExoPlayer(
            context = this,
            eqManager = eqManager,
            visualizerManager = visualizerManager,
            onDecoderInitialized = { decoder ->
                Log.i(TAG, "Media3 Service Decoder initialized: $decoder")
            },
            onAudioError = { errorMsg ->
                Log.e(TAG, "Media3 Service Audio Error: $errorMsg")
            }
        )

        val exo = engineResult.player
        player = exo

        exo.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val sid = exo.audioSessionId
                    if (sid > 0) {
                        Log.i("MBoxVis", "visualizer start sid=$sid ok=true")
                        visualizerManager.start(sid)
                    }
                }
            }
        })

        // Attach AnalyticsListener to capture real audio decoder, format, and track config
        exo.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long
            ) {
                Log.i(TAG, "Real Audio Decoder Initialized: $decoderName")
            }

            override fun onAudioInputFormatChanged(
                eventTime: AnalyticsListener.EventTime,
                format: Format,
                decoderReuseEvaluation: androidx.media3.exoplayer.DecoderReuseEvaluation?
            ) {
                Log.i(TAG, "Real Audio Input Format: codec=${format.sampleMimeType}, rate=${format.sampleRate}Hz, bitrate=${format.bitrate}")
            }
        })

        val sessionCallback = object : MediaSession.Callback {
            override fun onSetMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>,
                startIndex: Int,
                startPositionMs: Long
            ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                Log.i(TAG, "MediaSession.Callback.onSetMediaItems called with ${mediaItems.size} items")

                if (mediaItems.isEmpty()) {
                    return Futures.immediateFuture(
                        MediaSession.MediaItemsWithStartPosition(emptyList(), startIndex, startPositionMs)
                    )
                }

                val item = mediaItems.first()
                val uri = item.localConfiguration?.uri

                // If media item already has a resolved stream URI, pass through immediately
                if (uri != null && uri.toString().isNotBlank() && !uri.toString().startsWith("https://www.youtube.com")) {
                    Log.i(TAG, "MediaItem already has resolved stream URI: host=${uri.host}")
                    return Futures.immediateFuture(
                        MediaSession.MediaItemsWithStartPosition(mediaItems, startIndex, startPositionMs)
                    )
                }

                // Otherwise resolve stream asynchronously via YouTubeMusicSource
                val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()

                serviceScope.launch {
                    try {
                        val trackId = item.mediaId
                        val title = item.mediaMetadata.title?.toString() ?: "Unknown Title"
                        val artist = item.mediaMetadata.artist?.toString() ?: "Unknown Artist"
                        val artworkUrl = item.mediaMetadata.artworkUri?.toString()

                        val sourceTrack = SourceTrack(
                            id = trackId,
                            title = title,
                            artist = artist,
                            album = "YouTube Music",
                            durationMs = item.mediaMetadata.extras?.getLong("durationMs") ?: 0L,
                            artworkUrl = artworkUrl,
                            sourceId = "youtube"
                        )

                        Log.i(TAG, "Resolving stream for '${sourceTrack.title}' in MediaSession.Callback...")
                        val resolvedStream = youtubeMusicSource.resolve(sourceTrack)

                        val qualityLabel = resolvedStream.toQualityLabel("YouTube Music")
                        val extras = (item.mediaMetadata.extras ?: Bundle()).apply {
                            putString("qualityLabel", qualityLabel)
                            putString("codec", resolvedStream.codec)
                            putInt("bitrateKbps", resolvedStream.bitrateKbps)
                            putInt("sampleRateHz", resolvedStream.sampleRateHz ?: 48000)
                            putBoolean("lossy", resolvedStream.lossy)
                        }

                        val resolvedMetadata = item.mediaMetadata.buildUpon()
                            .setExtras(extras)
                            .build()

                        val resolvedItem = item.buildUpon()
                            .setUri(Uri.parse(resolvedStream.url))
                            .setMimeType("audio/${resolvedStream.container}")
                            .setMediaMetadata(resolvedMetadata)
                            .build()

                        Log.i(TAG, "Resolved MediaItem successfully: qualityLabel=$qualityLabel")
                        future.set(
                            MediaSession.MediaItemsWithStartPosition(listOf(resolvedItem), startIndex, startPositionMs)
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error resolving MediaItem in MediaSession.Callback: ${e.message}", e)
                        future.setException(e)
                    }
                }

                return future
            }
        }

        mediaSession = MediaSession.Builder(this, exo)
            .setCallback(sessionCallback)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = player
        if (p == null || !p.playWhenReady || p.playbackState == Player.STATE_IDLE || p.playbackState == Player.STATE_ENDED) {
            Log.i(TAG, "Task removed and player idle -> stopping StreamMediaSessionService")
            visualizerManager.stop()
            stopSelf()
        }
    }

    override fun onDestroy() {
        Log.i(TAG, "StreamMediaSessionService onDestroy()")
        serviceScope.cancel()
        visualizerManager.stop()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        player = null
        super.onDestroy()
    }
}
