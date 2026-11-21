package com.goreecloud.gifs.data

/** Bounded retries for idempotent Openverse GIF search requests only. */
object OpenverseRetryPolicy {
    const val MAX_RETRIES = 2
    const val MAX_DELAY_MILLIS = 10_000L

    fun shouldRetry(statusCode: Int, retriesSoFar: Int): Boolean =
        retriesSoFar in 0 until MAX_RETRIES &&
            statusCode in setOf(408, 429, 500, 502, 503, 504)

    fun delayMillis(retriesSoFar: Int, retryAfterHeader: String?): Long {
        val serverSeconds = retryAfterHeader?.trim()?.toLongOrNull()
        if (serverSeconds != null && serverSeconds >= 0) {
            // Bounded even if the server returns an enormous Retry-After.
            return if (serverSeconds > MAX_DELAY_MILLIS / 1000)
                MAX_DELAY_MILLIS else serverSeconds * 1000
        }
        return (750L * (1L shl retriesSoFar.coerceIn(0, MAX_RETRIES)))
            .coerceAtMost(MAX_DELAY_MILLIS)
    }
}
