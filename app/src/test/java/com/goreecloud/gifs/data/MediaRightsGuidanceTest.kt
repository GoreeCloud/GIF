package com.goreecloud.gifs.data

import org.junit.Assert.*
import org.junit.Test

class MediaRightsGuidanceTest {
    private fun media(id: String = "openverse:example1", license: String = "Unknown") =
        MediaItem(
            id = id, title = "Example", kind = MediaKind.GIF,
            url = "https://example.org/a.gif", thumbnail = "https://example.org/t.png",
            credit = "", sourceUrl = "https://example.org", license = license
        )

    @Test fun importedMediaRequiresExplicitRightsAwareness() {
        val message = MediaRightsGuidance.forItem(media("local:abc", "User-supplied media"))
        assertTrue(message.contains("permission"))
        assertTrue(message.contains("Imported"))
    }

    @Test fun shareAlikeIsMoreSpecificThanAttribution() {
        val message = MediaRightsGuidance.forItem(media(license = "CC BY-SA 4.0"))
        assertTrue(message.contains("share-alike"))
        assertTrue(message.contains("creator"))
    }

    @Test fun attributionLicenseDoesNotPromiseUnrestrictedUse() {
        val message = MediaRightsGuidance.forItem(media(license = "cc by 4.0"))
        assertTrue(message.contains("Attribution"))
        assertTrue(message.contains("conditions"))
    }

    @Test fun publicDomainNoticeStillRequiresCheckingThirdPartyRights() {
        for (license in listOf("CC0 1.0", "Public domain")) {
            assertTrue(MediaRightsGuidance.forItem(media(license = license))
                .contains("third-party rights"))
        }
    }

    @Test fun missingOrUnknownLicenseIsNotPresentedAsVerified() {
        for (license in listOf("", "See source for license", "UNKNOWN", "Unspecified")) {
            val message = MediaRightsGuidance.forItem(media(license = license))
            assertTrue(message.contains("not verified"))
        }
    }

    @Test fun unfamiliarLicenseStaysNeutral() {
        val message = MediaRightsGuidance.forItem(media(license = "Example Custom License"))
        assertTrue(message.contains("not independently verified"))
        assertFalse(message.contains("free to use"))
    }
}
