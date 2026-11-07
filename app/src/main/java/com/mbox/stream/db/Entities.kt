package com.mbox.stream.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val artist: String,
    val album: String = "YouTube Music",
    val durationMs: Long,
    val artworkUrl: String?,
    val localArtPath: String? = null,
    val codec: String = "Opus",
    val bitrateKbps: Int = 160,
    val sampleRateHz: Int = 48000,
    val container: String = "webm",
    val itag: Int = 251,
    val lossy: Boolean = true
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val kind: String = "USER"
)

@Entity(tableName = "playlist_items")
data class PlaylistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: String,
    val videoId: String,
    val position: Int,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "liked")
data class LikedEntity(
    @PrimaryKey val videoId: String,
    val likedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val videoId: String,
    val playedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val videoId: String,
    val itag: Int = 251,
    val codec: String = "Opus",
    val bitrateKbps: Int = 160,
    val sampleRateHz: Int = 48000,
    val container: String = "webm",
    val bytes: Long = 0L,
    val state: String = "QUEUED",
    val progress: Float = 0f,
    val error: String? = null,
    val requestedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
