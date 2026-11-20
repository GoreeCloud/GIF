package com.goreecloud.gifs.data

import org.junit.Assert.*
import org.junit.Test

class StickerPaginationTest {
    private val entries = (1..130).toList()

    @Test fun slicesLocalCatalogIntoOrderedPagesWithoutDuplicates() {
        val first = StickerPagination.page(entries, 1)
        val second = StickerPagination.page(entries, 2)
        val third = StickerPagination.page(entries, 3)
        assertEquals(60, first.items.size)
        assertEquals(60, second.items.size)
        assertEquals(10, third.items.size)
        assertTrue(first.hasMore)
        assertTrue(second.hasMore)
        assertFalse(third.hasMore)
        assertEquals(entries, first.items + second.items + third.items)
    }

    @Test fun outOfRangePagesDoNotRepeatPreviousResults() {
        assertEquals(emptyList<Int>(), StickerPagination.page(entries, 4).items)
        assertFalse(StickerPagination.page(entries, 4).hasMore)
        assertEquals(entries.take(60), StickerPagination.page(entries, 0).items)
        assertEquals(emptyList<Int>(), StickerPagination.page(entries, 999).items)
    }

    @Test fun respectsFilteredResultsAndTinyCatalogs() {
        val filtered = entries.filter { it % 17 == 0 }
        val result = StickerPagination.page(filtered)
        assertEquals(filtered, result.items)
        assertFalse(result.hasMore)
        assertEquals(emptyList<Int>(), StickerPagination.page(emptyList<Int>()).items)
    }

    @Test fun pageSizeIsBounded() {
        assertThrows(IllegalArgumentException::class.java) {
            StickerPagination.page(entries, pageSize = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            StickerPagination.page(entries, pageSize = StickerPagination.PAGE_SIZE + 1)
        }
    }
}
