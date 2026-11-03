package com.mbox.source.youtube

import android.util.Log
import com.mediaplayer.data.model.MusicSource
import com.mediaplayer.data.model.ResolvedStream
import com.mediaplayer.data.model.SourceTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.net.URI
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeMusicSource @Inject constructor() : MusicSource {

    override val id: String = "youtube"
    override val displayName: String = "YouTube Music"
    override val unofficial: Boolean = true

    companion object {
        private const val TAG = "MBoxStream"
        private val initialized = AtomicBoolean(false)

        fun ensureInitialized() {
            if (initialized.compareAndSet(false, true)) {
                try {
                    NewPipe.init(
                        YouTubeDownloader.create(),
                        Localization.fromLocale(Locale.getDefault())
                    )
                    Log.i(TAG, "NewPipeExtractor initialized successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "NewPipeExtractor init error: ${e.message}", e)
                }
            }
        }
    }

    init {
        ensureInitialized()
    }

    override suspend fun search(query: String): List<SourceTrack> = withContext(Dispatchers.IO) {
        ensureInitialized()
        Log.i(TAG, "search() query: '$query'")
        val service = ServiceList.YouTube
        val searchInfo = SearchInfo.getInfo(
            service,
            service.searchQHFactory.fromQuery(query, listOf("music_songs"), "")
        )

        val results = searchInfo.relatedItems
            .filterIsInstance<StreamInfoItem>()
            .map { item ->
                SourceTrack(
                    id = item.url ?: "",
                    title = item.name ?: "Unknown Title",
                    artist = item.uploaderName ?: "Unknown Artist",
                    album = "YouTube Music",
                    durationMs = if (item.duration > 0) item.duration * 1000L else 0L,
                    artworkUrl = item.thumbnails?.firstOrNull()?.url,
                    sourceId = id
                )
            }
        Log.i(TAG, "search() returned ${results.size} tracks")
        results
    }

    override suspend fun resolve(track: SourceTrack): ResolvedStream = withContext(Dispatchers.IO) {
        ensureInitialized()
        Log.i(TAG, "resolve() start for ID: ${track.id} (${track.title})")
        val streamInfo = try {
            StreamInfo.getInfo(ServiceList.YouTube, track.id)
        } catch (e: Exception) {
            Log.w(TAG, "resolve() initial attempt failed for ${track.title}: ${e.message}. Retrying in 500ms...")
            delay(500)
            try {
                StreamInfo.getInfo(ServiceList.YouTube, track.id)
            } catch (retryEx: Exception) {
                Log.e(TAG, "resolve() retry failed for ${track.title}: ${retryEx.message}", retryEx)
                throw retryEx
            }
        }

        val audioStreams: List<AudioStream> = streamInfo.audioStreams
        Log.i(TAG, "resolve() found ${audioStreams.size} audio streams")

        if (audioStreams.isEmpty()) {
            val err = IllegalStateException("No audio streams available for this song")
            Log.e(TAG, "resolve() error: ${err.message}", err)
            throw err
        }

        // Pick the best AUDIO-ONLY stream: Opus over AAC, highest bitrate. Never video.
        val bestStream = audioStreams
            .sortedWith(
                compareByDescending<AudioStream> { isOpus(it) }
                    .thenByDescending { it.averageBitrate }
            )
            .first()

        val codecName = when {
            isOpus(bestStream) -> "Opus"
            isAac(bestStream) -> "AAC"
            else -> bestStream.format?.name ?: "Audio"
        }

        val formatName = bestStream.format?.name?.lowercase() ?: ""
        val container = when {
            formatName.contains("webm") -> "webm"
            formatName.contains("m4a") -> "m4a"
            else -> "mp4"
        }

        val streamUrl = bestStream.url ?: run {
            val err = IllegalStateException("Stream URL is null")
            Log.e(TAG, "resolve() error: ${err.message}", err)
            throw err
        }

        val hostOnly = try { URI(streamUrl).host ?: "unknown" } catch (_: Exception) { "unknown" }

        val resolved = ResolvedStream(
            url = streamUrl,
            codec = codecName,
            bitrateKbps = if (bestStream.averageBitrate > 0) bestStream.averageBitrate else 160,
            sampleRateHz = 44100,
            container = container,
            lossy = true,
            expiresAtMs = System.currentTimeMillis() + (4 * 60 * 60 * 1000L)
        )

        Log.i(
            TAG,
            "resolve() result: codec=${resolved.codec}, bitrate=${resolved.bitrateKbps} kbps, container=${resolved.container}, host=$hostOnly"
        )
        resolved
    }

    private fun isOpus(stream: AudioStream): Boolean {
        val formatName = stream.format?.name ?: ""
        val codec = stream.codec ?: ""
        return formatName.contains("opus", ignoreCase = true) || codec.contains("opus", ignoreCase = true)
    }

    private fun isAac(stream: AudioStream): Boolean {
        val formatName = stream.format?.name ?: ""
        val codec = stream.codec ?: ""
        return formatName.contains("aac", ignoreCase = true) ||
                formatName.contains("m4a", ignoreCase = true) ||
                codec.contains("mp4a", ignoreCase = true)
    }
}
