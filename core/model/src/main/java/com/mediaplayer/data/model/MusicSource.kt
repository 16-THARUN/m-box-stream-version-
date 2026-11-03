package com.mediaplayer.data.model

interface MusicSource {
    val id: String
    val displayName: String
    val unofficial: Boolean
    suspend fun search(query: String): List<SourceTrack>
    suspend fun resolve(track: SourceTrack): ResolvedStream
}
