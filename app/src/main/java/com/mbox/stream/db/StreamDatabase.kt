package com.mbox.stream.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        LikedEntity::class,
        HistoryEntity::class,
        DownloadEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class StreamDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun likedDao(): LikedDao
    abstract fun historyDao(): HistoryDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `songs` (`videoId` TEXT NOT NULL, `title` TEXT NOT NULL, `artist` TEXT NOT NULL, `album` TEXT NOT NULL DEFAULT 'YouTube Music', `durationMs` INTEGER NOT NULL, `artworkUrl` TEXT, `localArtPath` TEXT, `codec` TEXT NOT NULL DEFAULT 'Opus', `bitrateKbps` INTEGER NOT NULL DEFAULT 160, `sampleRateHz` INTEGER NOT NULL DEFAULT 48000, `container` TEXT NOT NULL DEFAULT 'webm', `itag` INTEGER NOT NULL DEFAULT 251, `lossy` INTEGER NOT NULL DEFAULT 1, PRIMARY KEY(`videoId`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `playlists` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `kind` TEXT NOT NULL DEFAULT 'USER', PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `playlist_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `playlistId` TEXT NOT NULL, `videoId` TEXT NOT NULL, `position` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `liked` (`videoId` TEXT NOT NULL, `likedAt` INTEGER NOT NULL, PRIMARY KEY(`videoId`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `history` (`videoId` TEXT NOT NULL, `playedAt` INTEGER NOT NULL, PRIMARY KEY(`videoId`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `downloads` (`videoId` TEXT NOT NULL, `itag` INTEGER NOT NULL DEFAULT 251, `codec` TEXT NOT NULL DEFAULT 'Opus', `bitrateKbps` INTEGER NOT NULL DEFAULT 160, `sampleRateHz` INTEGER NOT NULL DEFAULT 48000, `container` TEXT NOT NULL DEFAULT 'webm', `bytes` INTEGER NOT NULL DEFAULT 0, `state` TEXT NOT NULL DEFAULT 'QUEUED', `progress` REAL NOT NULL DEFAULT 0.0, `error` TEXT, `requestedAt` INTEGER NOT NULL, `completedAt` INTEGER, PRIMARY KEY(`videoId`))")
            }
        }

        fun buildDatabase(context: Context): StreamDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                StreamDatabase::class.java,
                "mbox_stream.db"
            )
            .addMigrations(MIGRATION_1_2)
            .build()
        }
    }
}
