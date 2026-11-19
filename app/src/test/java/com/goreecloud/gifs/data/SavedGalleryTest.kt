package com.goreecloud.gifs.data

import org.junit.Assert.*
import org.junit.Test

class SavedGalleryTest {
    private val items = listOf(
        MediaItem(
            id = "a", title = "Happy dance", kind = MediaKind.GIF,
            url = "https://example.org/a.gif", thumbnail = "https://example.org/a.jpg",
            credit = "Alice Example", sourceUrl = "https://example.org/a", license = "CC BY 4.0"
        ),
        MediaItem(
            id = "b", title = "Cat face", kind = MediaKind.STICKER,
            url = "https://example.org/b.png", thumbnail = "https://example.org/b.png",
            credit = "OpenMoji contributors", sourceUrl = "https://example.org/b", license = "CC BY-SA 4.0"
        )
    )

    @Test fun allMediaVisibleWithoutFilters() {
        assertEquals(listOf("a", "b"), SavedGallery.filter(items, "", SavedKind.ALL).map { it.id })
    }

    @Test fun filterByTypeWithoutChangingOriginalList() {
        assertEquals(listOf("a"), SavedGallery.filter(items, "", SavedKind.GIF).map { it.id })
        assertEquals(listOf("b"), SavedGallery.filter(items, "", SavedKind.STICKER).map { it.id })
        assertEquals(2, items.size)
    }

    @Test fun searchTitleCreatorAndLicenseCaseInsensitively() {
        assertEquals(listOf("a"), SavedGallery.filter(items, "  HAPPY ", SavedKind.ALL).map { it.id })
        assertEquals(listOf("a"), SavedGallery.filter(items, "alice", SavedKind.ALL).map { it.id })
        assertEquals(listOf("b"), SavedGallery.filter(items, "by-sa", SavedKind.ALL).map { it.id })
        assertTrue(SavedGallery.filter(items, "unknown", SavedKind.ALL).isEmpty())
    }

    @Test fun requestingAppMimeRestrictionStillApplies() {
        assertEquals(listOf("a"), SavedGallery.filter(items, "", SavedKind.ALL, "image/gif").map { it.id })
        assertEquals(listOf("b"), SavedGallery.filter(items, "", SavedKind.ALL, "image/png").map { it.id })
        assertTrue(SavedGallery.filter(items, "", SavedKind.GIF, "image/png").isEmpty())
    }
}
