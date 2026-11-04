package com.mbox.stream.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ResolvingDataSourceRetryTest {

    @Test
    fun testRetryOnHttpError() {
        var attempts = 0

        fun resolveWithRetry(): String {
            var retryCount = 0
            while (retryCount <= 2) {
                attempts++
                try {
                    if (attempts == 1) {
                        throw IOException("HTTP 403 Forbidden")
                    }
                    return "https://rr1.googlevideo.com/videoplayback?id=123"
                } catch (e: Exception) {
                    retryCount++
                    if (retryCount > 2) throw e
                }
            }
            throw IllegalStateException("Failed after retries")
        }

        val url = resolveWithRetry()
        assertEquals(2, attempts)
        assertTrue(url.contains("googlevideo.com"))
    }
}
