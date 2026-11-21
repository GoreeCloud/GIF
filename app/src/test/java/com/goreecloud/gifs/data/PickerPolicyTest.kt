package com.goreecloud.gifs.data

import org.junit.Assert.*
import org.junit.Test

class PickerPolicyTest {
    private fun item(kind: MediaKind): MediaItem = MediaItem(
        id = kind.name, title = "Test", kind = kind,
        url = "https://example.org/a", thumbnail = "https://example.org/a",
        credit = "Test", sourceUrl = "https://example.org", license = "CC BY"
    )

    @Test fun mimeTypeFilteringHonorsRequest() {
        assertTrue(PickerPolicy.accepts("image/gif", item(MediaKind.GIF)))
        assertFalse(PickerPolicy.accepts("image/gif", item(MediaKind.STICKER)))
        assertTrue(PickerPolicy.accepts("image/png", item(MediaKind.STICKER)))
        assertFalse(PickerPolicy.accepts("image/png", item(MediaKind.GIF)))
        assertTrue(PickerPolicy.accepts("image/*", item(MediaKind.GIF)))
        assertTrue(PickerPolicy.accepts("image/*", item(MediaKind.STICKER)))
        assertFalse(PickerPolicy.accepts("image/jpeg", item(MediaKind.GIF)))
    }

    @Test fun defaultTabMatchesRequestedType() {
        assertEquals(MediaKind.STICKER, PickerPolicy.defaultKind("image/png"))
        assertEquals(MediaKind.GIF, PickerPolicy.defaultKind("image/gif"))
        assertEquals(MediaKind.GIF, PickerPolicy.defaultKind("image/*"))
    }

    @Test fun rejectsUnimplementedMimeTypes() {
        assertTrue(PickerPolicy.isSupported("IMAGE/GIF"))
        assertTrue(PickerPolicy.isSupported("image/gif"))
        assertTrue(PickerPolicy.isSupported("image/png"))
        assertTrue(PickerPolicy.isSupported("image/*"))
        assertFalse(PickerPolicy.isSupported("image/jpeg"))
        assertFalse(PickerPolicy.isSupported("*/*"))
        assertFalse(PickerPolicy.isSupported(null))
    }
}
