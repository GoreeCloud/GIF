package com.goreecloud.gifs.data

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class MediaFilesTest {
    @Test fun previouslyCachedMediaMustStillMatchExpectedFormat() {
        val file = File.createTempFile("goreecloud-cached-", ".gif")
        try {
            file.writeBytes(TestMediaHeaders.gif())
            assertTrue(MediaFiles.isValidStoredMedia(file, MediaKind.GIF))
            assertFalse(MediaFiles.isValidStoredMedia(file, MediaKind.STICKER))
            file.writeBytes("INVALID!".toByteArray())
            assertFalse(MediaFiles.isValidStoredMedia(file, MediaKind.GIF))
        } finally { file.delete() }
    }

    @Test fun emptyAndTruncatedOfflineFilesAreRejected() {
        val file = File.createTempFile("goreecloud-saved-", ".png")
        try {
            file.writeBytes(byteArrayOf())
            assertFalse(MediaFiles.isValidStoredMedia(file, MediaKind.STICKER))
            file.writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47))
            assertFalse(MediaFiles.isValidStoredMedia(file, MediaKind.STICKER))
            file.writeBytes(TestMediaHeaders.png())
            assertTrue(MediaFiles.isValidStoredMedia(file, MediaKind.STICKER))
        } finally { file.delete() }
    }

    @Test fun oversizedStoredMediaIsRejectedWithoutReadingTheWholeFile() {
        val file = File.createTempFile("goreecloud-oversize-", ".gif")
        try {
            java.io.RandomAccessFile(file, "rw").use { output ->
                output.write("GIF89a00".toByteArray(Charsets.US_ASCII))
                output.setLength(ImportPolicy.MAX_BYTES + 1L)
            }
            assertFalse(MediaFiles.isValidStoredMedia(file, MediaKind.GIF))
        } finally { file.delete() }
    }

    @Test fun gifSignatureIsChecked() {
        val file = File.createTempFile("goreecloud", ".gif")
        try {
            file.writeBytes("GIF89a00".toByteArray())
            assertTrue(MediaFiles.matchesMagic(file, MediaKind.GIF))
            assertFalse(MediaFiles.matchesMagic(file, MediaKind.STICKER))
            file.writeBytes("<script>".toByteArray())
            assertFalse(MediaFiles.matchesMagic(file, MediaKind.GIF))
        } finally { file.delete() }
    }
}
