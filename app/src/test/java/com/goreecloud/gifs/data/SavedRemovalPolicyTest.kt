package com.goreecloud.gifs.data

import org.junit.Assert.*
import org.junit.Test

class SavedRemovalPolicyTest {
    private fun item(id: String) = MediaItem(
        id, "Stickers", MediaKind.STICKER, "", "", "", "", "User-provided"
    )

    @Test fun onlyRemovingAnExistingSavedItemNeedsConfirmation() {
        val ids = setOf("local:a", "openmoji:b")
        assertTrue(SavedRemovalPolicy.requiresConfirmation(ids, item("local:a")))
        assertTrue(SavedRemovalPolicy.requiresConfirmation(ids, item("openmoji:b")))
        assertFalse(SavedRemovalPolicy.requiresConfirmation(ids, item("openmoji:c")))
    }

    @Test fun deletingImportedPrivateMediaExplainsIrreversibility() {
        assertTrue(SavedRemovalPolicy.warning(item("local:a")).contains("no remote source"))
        assertTrue(SavedRemovalPolicy.warning(item("local:a")).contains("not deleted"))
        assertFalse(SavedRemovalPolicy.warning(item("openmoji:b")).contains("no remote source"))
    }
}
