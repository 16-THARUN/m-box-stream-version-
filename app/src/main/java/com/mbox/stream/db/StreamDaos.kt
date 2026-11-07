package com.mbox.stream.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    @Query("SELECT * FROM songs WHERE videoId = :videoId")
    suspend fun getSong(videoId: String): SongEntity?

    @Query("SELECT * FROM songs WHERE videoId IN (:videoIds)")
    suspend fun getSongs(videoIds: List<String>): List<SongEntity>
}

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: PlaylistItemEntity)

    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun getPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY position ASC")
    fun getPlaylistItems(playlistId: String): Flow<List<PlaylistItemEntity>>

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun deletePlaylistItem(playlistId: String, videoId: String)
}

@Dao
interface LikedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLiked(liked: LikedEntity)

    @Query("DELETE FROM liked WHERE videoId = :videoId")
    suspend fun deleteLiked(videoId: String)

    @Query("SELECT * FROM liked ORDER BY likedAt DESC")
    fun getLiked(): Flow<List<LikedEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM liked WHERE videoId = :videoId)")
    fun isLiked(videoId: String): Flow<Boolean>
}

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)

    @Query("SELECT * FROM history ORDER BY playedAt DESC LIMIT 100")
    fun getHistory(): Flow<List<HistoryEntity>>
}

@Dao
interface DownloadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)

    @Query("SELECT * FROM downloads ORDER BY requestedAt DESC")
    fun getDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE videoId = :videoId")
    suspend fun getDownload(videoId: String): DownloadEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM downloads WHERE videoId = :videoId AND state = 'DONE')")
    fun isDownloaded(videoId: String): Flow<Boolean>

    @Query("DELETE FROM downloads WHERE videoId = :videoId")
    suspend fun deleteDownload(videoId: String)

    @Query("DELETE FROM downloads")
    suspend fun deleteAllDownloads()
}
