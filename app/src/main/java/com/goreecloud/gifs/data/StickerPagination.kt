package com.goreecloud.gifs.data

/** A bounded page from the local, already bundled OpenMoji metadata catalog. */
data class StickerSearchPage<T>(
    val items: List<T>,
    val page: Int,
    val hasMore: Boolean
)

object StickerPagination {
    const val PAGE_SIZE = 60
    const val MAX_PAGES = 100

    fun <T> page(
        matching: List<T>,
        requestedPage: Int = 1,
        pageSize: Int = PAGE_SIZE
    ): StickerSearchPage<T> {
        require(pageSize in 1..PAGE_SIZE) { "Invalid sticker page size" }
        val page = requestedPage.coerceIn(1, MAX_PAGES)
        val first = (page - 1) * pageSize
        if (first >= matching.size) return StickerSearchPage(emptyList(), page, false)
        val end = minOf(first + pageSize, matching.size)
        return StickerSearchPage(
            items = matching.subList(first, end).toList(),
            page = page,
            hasMore = end < matching.size && page < MAX_PAGES
        )
    }
}
