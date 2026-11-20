package com.goreecloud.gifs.data

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class MediaHeadersTest {
    private fun withFile(bytes: ByteArray, block: (File) -> Unit) {
        val file = File.createTempFile("goreecloud-header-", ".bin")
        try {
            file.writeBytes(bytes)
            block(file)
        } finally { file.delete() }
    }

    @Test fun validDimensionBoundedGifAndPngHeadersAreAccepted() {
        withFile(TestMediaHeaders.gif(100, 100)) {
            assertTrue(MediaHeaders.plausible(it, MediaKind.GIF))
            assertFalse(MediaHeaders.plausible(it, MediaKind.STICKER))
        }
        withFile(TestMediaHeaders.png(618, 618)) {
            assertTrue(MediaHeaders.plausible(it, MediaKind.STICKER))
            assertFalse(MediaHeaders.plausible(it, MediaKind.GIF))
        }
    }

    @Test fun zeroAndExcessiveDimensionsAreRejected() {
        for (dimensions in listOf(0 to 100, 100 to 0, 8192 to 8192, 8193 to 2)) {
            val (width, height) = dimensions
            withFile(TestMediaHeaders.gif(width, height)) {
                assertFalse(MediaHeaders.plausible(it, MediaKind.GIF))
            }
            withFile(TestMediaHeaders.png(width, height)) {
                assertFalse(MediaHeaders.plausible(it, MediaKind.STICKER))
            }
        }
    }

    @Test fun corruptPngIhdrAndCrcAreRejected() {
        withFile(TestMediaHeaders.png().apply { this[29] = (this[29].toInt() xor 1).toByte() }) {
            assertFalse(MediaHeaders.plausible(it, MediaKind.STICKER))
        }
        withFile(TestMediaHeaders.png().apply { this[12] = 'X'.code.toByte() }) {
            assertFalse(MediaHeaders.plausible(it, MediaKind.STICKER))
        }
        withFile(TestMediaHeaders.png().copyOfRange(0, 20)) {
            assertFalse(MediaHeaders.plausible(it, MediaKind.STICKER))
        }
    }

    @Test fun truncatedAndUnknownContentIsRejected() {
        withFile("GIF89a00".toByteArray(Charsets.US_ASCII)) {
            assertFalse(MediaHeaders.plausible(it, MediaKind.GIF))
        }
        withFile("<script>not-an-image".toByteArray()) {
            assertFalse(MediaHeaders.plausible(it, MediaKind.GIF))
            assertFalse(MediaHeaders.plausible(it, MediaKind.STICKER))
        }
    }
}
