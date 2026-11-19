package com.goreecloud.gifs.data

/** Supported single-image Android ACTION_GET_CONTENT requests. */
object PickerPolicy {
    fun isSupported(requestedMime: String?): Boolean =
        requestedMime?.lowercase() in setOf("image/gif", "image/png", "image/*")

    fun defaultKind(requestedMime: String?): MediaKind =
        if (requestedMime.equals("image/png", ignoreCase = true)) MediaKind.STICKER else MediaKind.GIF

    fun accepts(requestedMime: String?, item: MediaItem): Boolean =
        when (requestedMime?.lowercase()) {
            "image/*" -> true
            "image/gif" -> item.kind == MediaKind.GIF
            "image/png" -> item.kind == MediaKind.STICKER
            else -> false
        }
}
