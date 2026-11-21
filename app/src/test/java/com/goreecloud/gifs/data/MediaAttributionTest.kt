package com.goreecloud.gifs.data

import org.junit.Assert.*
import org.junit.Test

class MediaAttributionTest {
    private fun media(credit: String = "Contributor", source: String = "https://example.org/gif") =
        MediaItem(
            id = "gif:1", title = "Wave animation", kind = MediaKind.GIF,
            url = "https://example.org/wave.gif", thumbnail = "https://example.org/thumb.jpg",
            credit = credit, sourceUrl = source, license = "CC BY 4.0"
        )

    @Test fun includesAttributionSourceAndLicense() {
        val credit = MediaAttribution.text(media())
        assertTrue(credit.contains("Wave animation"))
        assertTrue(credit.contains("Contributor"))
        assertTrue(credit.contains("License: CC BY 4.0"))
        assertTrue(credit.contains("Source: https://example.org/gif"))
    }

    @Test fun neverCopiesUnsafeSourceLinks() {
        val credit = MediaAttribution.text(media(source = "http://192.168.1.10/secret"))
        assertFalse(credit.contains("192.168"))
        assertFalse(credit.contains("Source:"))
    }

    @Test fun missingAuthorUsesClearFallback() {
        assertTrue(MediaAttribution.text(media(credit = " ")).contains("Creator not supplied"))
    }
}
