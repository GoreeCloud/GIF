package com.goreecloud.gifs.data

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class MediaFilesTest {
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
