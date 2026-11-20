package com.goreecloud.gifs.data

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ShareCacheQuotaTest {
    private fun testDir() = Files.createTempDirectory("goreecloud-gif-cache-").toFile()
    private fun file(dir: File, character: Char, bytes: Int, age: Long, now: Long): File =
        File(dir, character.toString().repeat(64) + ".png").apply {
            writeBytes(ByteArray(bytes) { character.code.toByte() })
            assertTrue(setLastModified(now - age))
        }

    @Test fun removesOldestCachedImageUntilUnderQuota() {
        val dir = testDir()
        try {
            val now = 2_000_000_000L
            val old = file(dir, 'a', 8, 3_600_000, now)
            val mid = file(dir, 'b', 8, 2_700_000, now)
            val fresh = file(dir, 'c', 8, 1_800_000, now)
            assertEquals(2, ShareCacheQuota.prune(dir, maxBytes = 8, nowMillis = now))
            assertFalse(old.exists())
            assertFalse(mid.exists())
            assertTrue(fresh.exists())
        } finally { dir.deleteRecursively() }
    }

    @Test fun preservesProtectedAndRecentMediaEvenWhenOverSoftQuota() {
        val dir = testDir()
        try {
            val now = 2_000_000_000L
            val old = file(dir, 'a', 8, 3_600_000, now)
            val protected = file(dir, 'b', 8, 3_600_000, now)
            val recent = file(dir, 'c', 8, 60_000, now)
            assertEquals(1, ShareCacheQuota.prune(
                dir, protected.name, maxBytes = 1, nowMillis = now
            ))
            assertFalse(old.exists())
            assertTrue(protected.exists())
            assertTrue(recent.exists())
        } finally { dir.deleteRecursively() }
    }

    @Test fun neverRemovesUnknownFilesOrPartialDownloads() {
        val dir = testDir()
        try {
            val now = 2_000_000_000L
            val temporary = File(dir, "download.part").apply {
                writeBytes(ByteArray(5))
                setLastModified(now - 3_600_000)
            }
            val unrelated = File(dir, "user.png").apply {
                writeBytes(ByteArray(5))
                setLastModified(now - 3_600_000)
            }
            assertEquals(0, ShareCacheQuota.prune(dir, maxBytes = 0, nowMillis = now))
            assertTrue(temporary.exists())
            assertTrue(unrelated.exists())
        } finally { dir.deleteRecursively() }
    }

    @Test fun emptyOrMissingCacheNeedsNoWork() {
        val dir = testDir()
        try {
            assertEquals(0, ShareCacheQuota.prune(dir, maxBytes = 0))
        } finally { dir.deleteRecursively() }
        assertEquals(0, ShareCacheQuota.prune(dir, maxBytes = 0))
    }
}
