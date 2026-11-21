package com.goreecloud.gifs.data

import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class MediaStoreExportCheckTest {
    @Test fun completeExportAndPublishedRowAreAccepted() {
        MediaStoreExportCheck.requireCopied(200L, 200L)
        MediaStoreExportCheck.requirePublished(1)
    }

    @Test fun partialAndEmptyCopiesAreRejected() {
        for ((expected, copied) in listOf(100L to 99L, 100L to 101L, 0L to 0L)) {
            try {
                MediaStoreExportCheck.requireCopied(expected, copied)
                fail("Partial or empty copy should fail")
            } catch (_: IOException) {
                // Expected, so caller must delete the pending row.
            }
        }
    }

    @Test fun missingPublishAcknowledgementIsRejected() {
        try {
            MediaStoreExportCheck.requirePublished(0)
            fail("A failed MediaStore update must not be reported as success")
        } catch (_: IOException) {
            // The calling transaction removes the uncommitted row.
        }
    }
}
