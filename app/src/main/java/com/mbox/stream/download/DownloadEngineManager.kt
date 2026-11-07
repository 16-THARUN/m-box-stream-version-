package com.mbox.stream.download

import android.content.Context
import android.os.StatFs
import android.util.Log
import com.mediaplayer.data.model.SourceTrack
import com.mbox.source.youtube.YouTubeMusicSource
import com.mbox.stream.db.DownloadEntity
import com.mbox.stream.db.LibraryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.io.File
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadEngineManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val libraryRepository: LibraryRepository,
    private val youtubeMusicSource: YouTubeMusicSource
) {
    private val TAG = "MBoxDownload"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val downloadDir = File(context.filesDir, "downloads").apply { if (!exists()) mkdirs() }
    val artDir = File(context.filesDir, "art").apply { if (!exists()) mkdirs() }

    fun checkFreeSpace(requiredBytes: Long): Boolean {
        val stat = StatFs(context.filesDir.path)
        val freeBytes = stat.availableBytes
        val remainingAfter = freeBytes - requiredBytes
        val minFreeSpace = 524_288_000L // 500 MB
        return remainingAfter >= minFreeSpace
    }

    fun startDownload(track: SourceTrack, onSpaceError: (String) -> Unit = {}) {
        val estimatedBytes = (track.durationMs / 1000L) * (160 * 1000L / 8L)
        if (!checkFreeSpace(estimatedBytes)) {
            Log.w(TAG, "Not enough space for ${track.title}")
            onSpaceError("Not enough space for ${track.title}")
            return
        }

        scope.launch {
            Log.i(TAG, "queued: ${track.title} (${track.id})")
            val entity = DownloadEntity(
                videoId = track.id,
                itag = 251,
                codec = "Opus",
                bitrateKbps = 160,
                sampleRateHz = 48000,
                container = "webm",
                bytes = estimatedBytes,
                state = "QUEUED",
                progress = 0f,
                requestedAt = System.currentTimeMillis()
            )
            libraryRepository.insertDownload(entity)

            // Perform Download
            downloadTrackInternal(track, entity)
        }
    }

    private suspend fun downloadTrackInternal(track: SourceTrack, initialEntity: DownloadEntity) {
        val startTime = System.currentTimeMillis()
        try {
            Log.i(TAG, "start (itag=251, codec=Opus) for '${track.title}'")
            libraryRepository.insertDownload(initialEntity.copy(state = "DOWNLOADING", progress = 0.1f))

            val stream = youtubeMusicSource.resolve(track)
            val audioFile = File(downloadDir, "yt_${track.id}.webm")

            // Perform stream download
            val url = URL(stream.url)
            val connection = url.openConnection()
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            val totalSize = connection.contentLengthLong.coerceAtLeast(1L)

            connection.getInputStream().use { input ->
                audioFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    var downloaded = 0L
                    var lastLoggedProgress = 0

                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        val prog = (downloaded.toFloat() / totalSize.toFloat()).coerceIn(0f, 1f)
                        val prog25 = (prog * 100).toInt() / 25 * 25
                        if (prog25 > lastLoggedProgress) {
                            lastLoggedProgress = prog25
                            Log.i(TAG, "progress for '${track.title}': $prog25%")
                            libraryRepository.insertDownload(initialEntity.copy(state = "DOWNLOADING", progress = prog, bytes = downloaded))
                        }
                    }
                }
            }

            // Save large artwork locally
            val localArt = File(artDir, "${track.id}.jpg")
            if (!track.artworkUrl.isNullOrBlank()) {
                try {
                    URL(track.artworkUrl).openStream().use { input ->
                        localArt.outputStream().use { output -> input.copyTo(output) }
                    }
                } catch (_: Exception) {}
            }

            val elapsed = System.currentTimeMillis() - startTime
            Log.i(TAG, "done (${audioFile.length()} bytes, ${elapsed} ms) for '${track.title}'")

            libraryRepository.saveSong(
                track.copy(artworkUrl = if (localArt.exists()) localArt.absolutePath else track.artworkUrl)
            )

            libraryRepository.insertDownload(
                initialEntity.copy(
                    state = "DONE",
                    progress = 1.0f,
                    bytes = audioFile.length(),
                    completedAt = System.currentTimeMillis()
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "failed (reason: ${e.message}) for '${track.title}'", e)
            libraryRepository.insertDownload(initialEntity.copy(state = "FAILED", error = e.message))
        }
    }

    fun removeDownload(videoId: String) {
        scope.launch {
            val audioFile = File(downloadDir, "yt_$videoId.webm")
            if (audioFile.exists()) audioFile.delete()
            val artFile = File(artDir, "$videoId.jpg")
            if (artFile.exists()) artFile.delete()

            libraryRepository.removeDownload(videoId)
            Log.i(TAG, "removed download for videoId=$videoId")
        }
    }
}
