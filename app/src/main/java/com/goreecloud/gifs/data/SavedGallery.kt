package com.goreecloud.gifs.data

/** View-only filters. Never delete or mutate persisted favorites when searching. */
enum class SavedKind { ALL, GIF, STICKER }
enum class SavedOrder { RECENT, OLDEST, TITLE }

object SavedGallery {
    fun filter(
        favorites: List<MediaItem>,
        query: String,
        kind: SavedKind,
        requestedMime: String? = null,
        order: SavedOrder = SavedOrder.OLDEST
    ): List<MediaItem> {
        val needle = query.trim()
        val matching = favorites.filter { item ->
            (kind == SavedKind.ALL || item.kind.name == kind.name) &&
                (needle.isBlank() ||
                    item.title.contains(needle, ignoreCase = true) ||
                    item.credit.contains(needle, ignoreCase = true) ||
                    item.license.contains(needle, ignoreCase = true)) &&
                (requestedMime == null || PickerPolicy.accepts(requestedMime, item))
        }
        return when (order) {
            SavedOrder.OLDEST -> matching
            // DataStore appends newly favorited or imported media at the end.
            SavedOrder.RECENT -> matching.asReversed()
            SavedOrder.TITLE -> matching.sortedWith(
                compareBy<MediaItem> { it.title.lowercase() }.thenBy { it.id }
            )
        }
    }
}
