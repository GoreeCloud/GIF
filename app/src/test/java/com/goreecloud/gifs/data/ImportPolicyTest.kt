package com.goreecloud.gifs.data

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ImportPolicyTest {
    @Test fun filenamesAreDisplayLabelsNotFilesystemPaths() {
        assertEquals("reaction", ImportPolicy.title("/not/real/reaction.gif", MediaKind.GIF))
        assertEquals("cat", ImportPolicy.title("C:\\fake\\cat.png", MediaKind.STICKER))
        assertEquals("wave", ImportPolicy.title("wave.gif", MediaKind.GIF))
        assertEquals("Imported sticker", ImportPolicy.title("", MediaKind.STICKER))
        assertEquals("Imported GIF", ImportPolicy.title(null, MediaKind.GIF))
    }

    @Test fun controlCharactersAreRemovedAndLengthBounded() {
        assertEquals("title", ImportPolicy.title("\u0000title\u0007.gif", MediaKind.GIF))
        assertTrue(ImportPolicy.title("a".repeat(250) + ".gif", MediaKind.GIF).length <= 80)
    }

    @Test fun importedMediaMustHaveGifOrPngSignature() {
        val file = File.createTempFile("goreecloud-import-", ".bin")
        try {
            file.writeBytes("GIF89a00".toByteArray(Charsets.US_ASCII))
            assertEquals(MediaKind.GIF, ImportPolicy.detectKind(file))
            file.writeBytes(byteArrayOf(
                0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
            ))
            assertEquals(MediaKind.STICKER, ImportPolicy.detectKind(file))
            file.writeBytes("<script>".toByteArray(Charsets.US_ASCII))
            assertNull(ImportPolicy.detectKind(file))
        } finally { file.delete() }
    }
}
