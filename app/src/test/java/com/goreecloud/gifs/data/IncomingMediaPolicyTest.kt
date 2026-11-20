package com.goreecloud.gifs.data

import org.junit.Assert.*
import org.junit.Test

class IncomingMediaPolicyTest {
    @Test fun acceptsExplicitSingleImageShares() {
        assertTrue(IncomingMediaPolicy.accepts(
            "android.intent.action.SEND", "IMAGE/GIF", "content", "images.provider"
        ))
        assertTrue(IncomingMediaPolicy.accepts(
            "android.intent.action.SEND", "image/png", "content", "other.provider"
        ))
    }

    @Test fun rejectsUnapprovedActionsAndMimes() {
        for (action in listOf(null, "", "android.intent.action.SEND_MULTIPLE", "android.intent.action.VIEW")) {
            assertFalse(IncomingMediaPolicy.accepts(action, "image/gif", "content", "images.provider"))
        }
        for (mime in listOf(null, "", "image/jpeg", "image/svg+xml", "image/*", "text/plain", "application/octet-stream")) {
            assertFalse(IncomingMediaPolicy.accepts(IncomingMediaPolicy.ACTION_SEND, mime, "content", "images.provider"))
        }
    }

    @Test fun rejectsLocalFilesAndExternalUrls() {
        for (scheme in listOf(null, "", "file", "http", "https")) {
            assertFalse(IncomingMediaPolicy.accepts(
                IncomingMediaPolicy.ACTION_SEND, "image/png", scheme, "images.provider"
            ))
        }
        assertFalse(IncomingMediaPolicy.accepts(
            IncomingMediaPolicy.ACTION_SEND, "image/png", "content", null
        ))
        assertFalse(IncomingMediaPolicy.accepts(
            IncomingMediaPolicy.ACTION_SEND, "image/png", "content", ""
        ))
    }
}
