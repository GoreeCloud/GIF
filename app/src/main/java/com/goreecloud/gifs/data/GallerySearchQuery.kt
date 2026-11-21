package com.goreecloud.gifs.data

/** Preserve the same effective query across every page of a media search. */
object GallerySearchQuery {
    fun normalize(query: String, kind: MediaKind): String {
        // No automatic remote search on launch or while the user is typing.
        // Sticker searches use bundled metadata; GIF queries are submitted explicitly.
        return query.trim()
    }
}
