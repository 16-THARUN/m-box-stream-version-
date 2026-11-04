package com.mbox.stream.radio

import com.mediaplayer.data.model.SourceTrack
import org.junit.Assert.assertEquals
import org.junit.Test

class RadioFilterTest {

    @Test
    fun testTitleDeduplication() {
        val title1 = RadioFilter.cleanTitle("Despacito (Official Music Video)")
        val title2 = RadioFilter.cleanTitle("despacito [audio]")
        assertEquals("despacito", title1)
        assertEquals("despacito", title2)
        assertEquals(title1, title2)
    }

    @Test
    fun testRadioFiltersOutHistoryAndShortTracks() {
        val candidates = listOf(
            SourceTrack("1", "Short Track", "Artist", "Album", 30_000L, null, "youtube"),
            SourceTrack("2", "Despacito", "Artist", "Album", 200_000L, null, "youtube"),
            SourceTrack("3", "Long Track", "Artist", "Album", 1_000_000L, null, "youtube"),
            SourceTrack("4", "Despacito (Official Video)", "Artist", "Album", 200_000L, null, "youtube"),
            SourceTrack("5", "New Track", "Artist", "Album", 180_000L, null, "youtube")
        )

        val history = listOf("2") // Despacito was played
        val filtered = RadioFilter.filterCandidateTracks(candidates, history, emptyList())

        // Track 1: skipped (too short)
        // Track 2: skipped (in history)
        // Track 3: skipped (too long)
        // Track 4: skipped (duplicate clean title 'despacito')
        // Track 5: accepted ('new track')
        assertEquals(1, filtered.size)
        assertEquals("5", filtered[0].id)
    }
}
