package com.goreecloud.gifs.data

import org.junit.Assert.*
import org.junit.Test

class SourcePolicyTest {
    @Test fun allowsSafeHttps() {
        assertTrue(SourcePolicy.isSafeHttps("https://upload.wikimedia.org/wikipedia/commons/c/c1/a.gif"))
    }
    @Test fun rejectsLocalNetworksAndDowngrades() {
        for (url in listOf(
            "http://example.org/a.gif", "https://localhost/a.gif",
            "https://127.0.0.1/a.gif", "https://192.168.1.10/a.gif",
            "https://10.0.0.1/a.gif", "https://172.19.3.2/a.gif",
            "https://[::1]/a.gif", "https://foo.local/a.gif",
            "https://user:pass@example.org/a.gif", "file:///tmp/a.gif"
        )) assertFalse(url, SourcePolicy.isSafeHttps(url))
    }
    @Test fun metadataRoundTrip() {
        val value = MediaItem("openmoji:1F600", "Smile", MediaKind.STICKER,
            "https://example.org/img.png", "https://example.org/thumb.png",
            "OpenMoji", "https://example.org", "CC BY-SA 4.0", "abc.png")
        assertEquals(value, MediaItem.fromJson(value.toJson()))
    }
}
