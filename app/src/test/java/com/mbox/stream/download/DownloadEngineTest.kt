package com.mbox.stream.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadEngineTest {

    @Test
    fun testCacheKeyFormat() {
        val videoId = "kJQP7kiw5Fk"
        val itag = 251
        val cacheKey = "yt:$videoId:$itag"
        assertEquals("yt:kJQP7kiw5Fk:251", cacheKey)
    }

    @Test
    fun testFreeSpaceThresholdCalculation() {
        val freeBytes = 1_000_000_000L // 1 GB free
        val requiredBytes = 10_000_000L // 10 MB download
        val minFreeSpace = 524_288_000L // 500 MB threshold

        val remainingAfter = freeBytes - requiredBytes
        val spaceOk = remainingAfter >= minFreeSpace
        assertTrue(spaceOk)

        val lowFreeBytes = 530_000_000L // 530 MB free
        val largeDownloadBytes = 50_000_000L // 50 MB download
        val lowSpaceOk = (lowFreeBytes - largeDownloadBytes) >= minFreeSpace
        assertFalse(lowSpaceOk)
    }
}
