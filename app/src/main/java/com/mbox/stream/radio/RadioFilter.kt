package com.mbox.stream.radio

import com.mediaplayer.data.model.SourceTrack

object RadioFilter {

    private val titleCleanRegex = Regex("(?i)\\s*\\((from|official|lyric|audio|video|music video|hd|4k)[^)]*\\)|\\s*\\[(official|lyric|audio|video|music video|hd|4k)[^\\]]*\\]")

    fun cleanTitle(title: String): String {
        return title.replace(titleCleanRegex, "").trim().lowercase()
    }

    fun filterCandidateTracks(
        candidates: List<SourceTrack>,
        historyTrackIds: List<String>,
        existingQueueTracks: List<SourceTrack>,
        historyTrackTitles: List<String> = emptyList()
    ): List<SourceTrack> {
        val recentHistorySet = historyTrackIds.takeLast(50).toSet()
        val recentHistoryTitlesSet = historyTrackTitles.map { cleanTitle(it) }.toSet()
        val filtered = mutableListOf<SourceTrack>()

        for (track in candidates) {
            val trackId = track.id
            val clean = cleanTitle(track.title)

            // 1. Skip if played in last 50 tracks (by ID or clean title)
            if (recentHistorySet.contains(trackId) || recentHistoryTitlesSet.contains(clean)) {
                continue
            }

            // 2. Skip items shorter than 60s (60,000ms) or longer than 15 min (900,000ms)
            if (track.durationMs in 1L..<60_000L || track.durationMs > 900_000L) {
                continue
            }

            // 3. Skip duplicate titles in queue or filtered
            if (existingQueueTracks.any { cleanTitle(it.title) == clean } ||
                filtered.any { cleanTitle(it.title) == clean }) {
                continue
            }

            filtered.add(track)
        }

        return filtered
    }
}
