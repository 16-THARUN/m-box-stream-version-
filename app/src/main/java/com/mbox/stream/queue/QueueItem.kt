package com.mbox.stream.queue

import com.mediaplayer.data.model.SourceTrack

enum class QueueSection {
    NOW_PLAYING,
    UP_NEXT,
    RADIO
}

data class QueueItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sourceTrack: SourceTrack,
    val section: QueueSection = QueueSection.UP_NEXT
)
