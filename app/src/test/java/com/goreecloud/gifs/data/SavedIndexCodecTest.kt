package com.goreecloud.gifs.data

import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class SavedIndexCodecTest {
    private fun item() = MediaItem(
        id = "openmoji:1F600", title = "Grinning face", kind = MediaKind.STICKER,
        url = "https://example.org/1F600.png",
        thumbnail = "https://example.org/t.png",
        credit = "OpenMoji", sourceUrl = "https://example.org", license = "CC BY-SA 4.0"
    )

    @Test fun validAndEmptySavedIndexCanRoundTrip() {
        assertEquals(emptyList<MediaItem>(), SavedIndexCodec.decode("[]"))
        val original = listOf(item())
        assertEquals(original, SavedIndexCodec.decode(SavedIndexCodec.encode(original)))
    }

    @Test fun malformedOrPartiallyUnreadableRecordsMustNotBeSilentlyDropped() {
        val examples = listOf(
            "", "broken", "{}", "[null]", "[42]", "[{}]",
            SavedIndexCodec.encode(listOf(item())).dropLast(3),
            "[" + item().toJson().toString() + ",{}]"
        )
        examples.forEach { bad ->
            try {
                SavedIndexCodec.decode(bad)
                fail("A damaged index must block writes instead of returning fewer records")
            } catch (error: IOException) {
                assertTrue(error.message.orEmpty().contains("unreadable"))
            }
        }
    }

    @Test fun oversizedIndexIsRejectedInsteadOfTruncated() {
        try {
            SavedIndexCodec.encode(List(SavedIndexCodec.MAX_ITEMS + 1) { item() })
            fail("The 10k record cap should be enforced before writing")
        } catch (error: IOException) {
            assertTrue(error.message.orEmpty().contains("full"))
        }
    }

    @Test fun invalidPersistedFilenameIsSanitizedWithoutDroppingTheRecord() {
        val corrupt = item().toJson().put("localFile", "../../secrets")
        val restored = SavedIndexCodec.decode("[" + corrupt + "]")
        assertEquals(1, restored.size)
        assertNull(restored.single().localFile)
    }
}
