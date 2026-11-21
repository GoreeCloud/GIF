package com.goreecloud.gifs.data

import org.junit.Assert.*
import org.junit.Test

class GallerySearchQueryTest {
    @Test fun emptyStickerSearchRemainsUnfilteredAcrossPages() {
        assertEquals("", GallerySearchQuery.normalize("", MediaKind.STICKER))
        assertEquals("", GallerySearchQuery.normalize("  \t  ", MediaKind.STICKER))
    }

    @Test fun gifSearchKeepsExistingDefault() {
        assertEquals("reaction", GallerySearchQuery.normalize("  ", MediaKind.GIF))
        assertEquals("reaction", GallerySearchQuery.normalize("", MediaKind.GIF))
    }

    @Test fun explicitQueriesAreTrimmedButNotReplaced() {
        assertEquals("sleeping face", GallerySearchQuery.normalize(" sleeping face ", MediaKind.STICKER))
        assertEquals("cat", GallerySearchQuery.normalize(" cat ", MediaKind.GIF))
    }
}
