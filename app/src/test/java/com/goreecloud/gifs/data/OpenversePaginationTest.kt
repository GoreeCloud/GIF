package com.goreecloud.gifs.data

import org.junit.Assert.*
import org.junit.Test

class OpenversePaginationTest {
    @Test fun anonymousSearchUsesAcceptedPageSize() {
        val url = OpenverseClient.buildSearchUrl("cat & dog", 4)
        assertEquals("20", url.queryParameter("page_size"))
        assertEquals("4", url.queryParameter("page"))
        assertEquals("cat & dog", url.queryParameter("q"))
        assertEquals("gif", url.queryParameter("extension"))
        assertEquals("false", url.queryParameter("mature"))
    }
    @Test fun pageRangeIsBounded() {
        assertEquals("1", OpenverseClient.buildSearchUrl("a", 0).queryParameter("page"))
        assertEquals("100", OpenverseClient.buildSearchUrl("a", 999).queryParameter("page"))
    }
    @Test fun paginationStopsAtLastPage() {
        assertTrue(OpenverseClient.parsePage("""{"page_count":3,"results":[]}""", 1).hasMore)
        assertFalse(OpenverseClient.parsePage("""{"page_count":3,"results":[]}""", 3).hasMore)
        assertFalse(OpenverseClient.parsePage("""{"results":[]}""", 1).hasMore)
    }
}
