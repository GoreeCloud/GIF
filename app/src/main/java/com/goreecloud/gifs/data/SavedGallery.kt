package com.goreecloud.gifs.data

/** View-only filters. Never delete or mutate persisted favorites when searching. */
enum class SavedKind { ALL, GIF, STICKER }

object SavedGallery {
    fun filter(
        favorites: List<MediaItem>,
        query: String,
        kind: SavedKind,
        requestedMime: String? = null
    ): List<MediaItem> {
        val needle = query.trim()
        return favorites.filter { item ->
            (kind == SavedKind.ALL || item.kind.name == kind.name) &&
                (needle.isBlank() ||
                    item.title.contains(needle, ignoreCase = true) ||
                    item.credit.contains(needle, ignoreCase = true) ||
                    item.license.contains(needle, ignoreCase = true)) &&
                (requestedMime == null || PickerPolicy.accepts(requestedMime, item))
        }
    }
}
