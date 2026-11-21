package com.goreecloud.gifs.data

/** Preserve the same effective query across every page of a media search. */
object GallerySearchQuery {
    fun normalize(query: String, kind: MediaKind): String {
        val trimmed = query.trim()
        return if (kind == MediaKind.GIF) trimmed.ifBlank { "reaction" } else trimmed
    }
}
