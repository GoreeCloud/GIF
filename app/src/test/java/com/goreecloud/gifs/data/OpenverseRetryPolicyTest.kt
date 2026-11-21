package com.goreecloud.gifs.data

import org.junit.Assert.*
import org.junit.Test

class OpenverseRetryPolicyTest {
    @Test fun retryOnlyTransientResponsesWithFiniteBound() {
        for (status in listOf(408, 429, 500, 502, 503, 504)) {
            assertTrue(OpenverseRetryPolicy.shouldRetry(status, 0))
            assertTrue(OpenverseRetryPolicy.shouldRetry(status, 1))
            assertFalse(OpenverseRetryPolicy.shouldRetry(status, 2))
        }
        for (status in listOf(200, 400, 401, 403, 404, 410, 422)) {
            assertFalse(OpenverseRetryPolicy.shouldRetry(status, 0))
        }
        assertFalse(OpenverseRetryPolicy.shouldRetry(429, -1))
    }

    @Test fun exponentialDelaysAndServerHintsHaveSensibleBounds() {
        assertEquals(750L, OpenverseRetryPolicy.delayMillis(0, null))
        assertEquals(1500L, OpenverseRetryPolicy.delayMillis(1, null))
        assertEquals(2000L, OpenverseRetryPolicy.delayMillis(0, "2"))
        assertEquals(0L, OpenverseRetryPolicy.delayMillis(0, "0"))
        assertEquals(10_000L, OpenverseRetryPolicy.delayMillis(0, "90000000"))
        assertEquals(750L, OpenverseRetryPolicy.delayMillis(0, "bad-value"))
    }
}
