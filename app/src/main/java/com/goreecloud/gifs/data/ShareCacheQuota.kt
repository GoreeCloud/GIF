package com.goreecloud.gifs.data

import java.io.File

/**
 * Best-effort size quota for temporary share/download media.
 *
 * Never removes app-private favorites. Only SHA-256-named cache files are eligible.
 * A grace period protects files recently shared through temporary content URI grants.
 */
object ShareCacheQuota {
    const val MAX_BYTES = 100L * 1024L * 1024L
    const val SHARE_GRACE_MILLIS = 15L * 60L * 1000L
    private val cacheName = Regex("^[0-9a-f]{64}\\.(gif|png)$")

    fun prune(
        directory: File,
        protectedName: String? = null,
        maxBytes: Long = MAX_BYTES,
        nowMillis: Long = System.currentTimeMillis()
    ): Int {
        require(maxBytes >= 0) { "Cache quota must not be negative." }
        if (!directory.isDirectory) return 0
        val files = (directory.listFiles() ?: return 0)
            .filter { it.isFile && cacheName.matches(it.name) }
            .sortedWith(compareBy<File> { it.lastModified() }.thenBy { it.name })
        var total = files.sumOf { it.length() }
        var removed = 0
        for (file in files) {
            if (total <= maxBytes) break
            if (file.name == protectedName) continue
            val modified = file.lastModified()
            // Don't revoke media that may still be opening in an Android share target.
            if (modified <= 0 || nowMillis - modified < SHARE_GRACE_MILLIS) continue
            val length = file.length()
            if (file.delete()) {
                total -= length
                removed++
            }
        }
        return removed
    }
}
