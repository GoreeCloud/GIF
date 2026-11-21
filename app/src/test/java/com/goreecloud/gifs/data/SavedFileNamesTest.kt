package com.goreecloud.gifs.data

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SavedFileNamesTest {
    @Test fun filenamesAreStableAndKindSpecific() {
        val gif = SavedFileNames.expected("local:example", MediaKind.GIF)
        val sticker = SavedFileNames.expected("local:example", MediaKind.STICKER)
        assertEquals(68, gif.length)
        assertEquals(68, sticker.length)
        assertTrue(gif.matches(Regex("[0-9a-f]{64}\\.gif")))
        assertTrue(sticker.matches(Regex("[0-9a-f]{64}\\.png")))
        assertFalse(gif == sticker)
    }

    @Test fun persistedMetadataCanOnlyReferenceItsOwnDeterministicFilename() {
        val saved = MediaItem(
            id = "local:demo", title = "A sticker", kind = MediaKind.STICKER,
            url = "", thumbnail = "", credit = "From device",
            sourceUrl = "", license = "User supplied",
            localFile = SavedFileNames.expected("local:demo", MediaKind.STICKER)
        )
        assertEquals(saved.localFile, MediaItem.fromJson(saved.toJson())?.localFile)
        val tampered = JSONObject(saved.toJson().toString()).put("localFile", "../../private/preferences.xml")
        val parsed = MediaItem.fromJson(tampered)
        assertNotNull(parsed)
        assertNull(parsed?.localFile)
        assertEquals(saved.id, parsed?.id)
    }

    @Test fun mismatchedIdsAndFormatsAreRejected() {
        val file = SavedFileNames.expected("first", MediaKind.GIF)
        assertFalse(SavedFileNames.matches("other", MediaKind.GIF, file))
        assertFalse(SavedFileNames.matches("first", MediaKind.STICKER, file))
        assertFalse(SavedFileNames.matches("first", MediaKind.GIF, "/tmp/" + file))
        assertFalse(SavedFileNames.matches("first", MediaKind.GIF, file.uppercase()))
    }
}
