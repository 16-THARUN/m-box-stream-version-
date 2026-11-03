package com.mediaplayer.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SourceTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationMs: Long,
    val artworkUrl: String? = null,
    val sourceId: String = "youtube"
)
