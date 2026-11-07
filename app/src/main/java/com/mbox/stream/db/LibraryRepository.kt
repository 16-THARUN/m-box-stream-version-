package com.mbox.stream.db

import com.mediaplayer.data.model.SourceTrack
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepository @Inject constructor(
    private val db: StreamDatabase
) {
    val likedSongs: Flow<List<LikedEntity>> = db.likedDao().getLiked()
    val recentlyPlayed: Flow<List<HistoryEntity>> = db.historyDao().getHistory()
    val downloads: Flow<List<DownloadEntity>> = db.downloadDao().getDownloads()
    val playlists: Flow<List<PlaylistEntity>> = db.playlistDao().getPlaylists()

    fun isDownloaded(videoId: String): Flow<Boolean> = db.downloadDao().isDownloaded(videoId)
    fun isLiked(videoId: String): Flow<Boolean> = db.likedDao().isLiked(videoId)

    suspend fun saveSong(track: SourceTrack) {
        val entity = SongEntity(
            videoId = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album ?: "YouTube Music",
            durationMs = track.durationMs,
            artworkUrl = track.artworkUrl
        )
        db.songDao().insertSong(entity)
    }

    suspend fun toggleLike(track: SourceTrack) {
        saveSong(track)
        db.likedDao().insertLiked(LikedEntity(videoId = track.id))
    }

    suspend fun unlike(videoId: String) {
        db.likedDao().deleteLiked(videoId)
    }

    suspend fun addHistory(track: SourceTrack) {
        saveSong(track)
        db.historyDao().insertHistory(HistoryEntity(videoId = track.id))
    }

    suspend fun createPlaylist(name: String, kind: String = "USER"): String {
        val id = java.util.UUID.randomUUID().toString()
        val playlist = PlaylistEntity(id = id, name = name, kind = kind)
        db.playlistDao().insertPlaylist(playlist)
        return id
    }

    suspend fun addTrackToPlaylist(playlistId: String, track: SourceTrack, position: Int) {
        saveSong(track)
        val item = PlaylistItemEntity(playlistId = playlistId, videoId = track.id, position = position)
        db.playlistDao().insertPlaylistItem(item)
    }

    suspend fun insertDownload(download: DownloadEntity) {
        db.downloadDao().insertDownload(download)
    }

    suspend fun removeDownload(videoId: String) {
        db.downloadDao().deleteDownload(videoId)
    }

    suspend fun clearAllDownloads() {
        db.downloadDao().deleteAllDownloads()
    }
}
