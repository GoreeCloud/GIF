package com.goreecloud.gifs.data

import java.io.File

/** Conservative local media limits and metadata normalization for user imports. */
object ImportPolicy {
    const val MAX_BYTES = 25L * 1024L * 1024L

    fun detectKind(file: File): MediaKind? = when {
        MediaFiles.matchesMagic(file, MediaKind.GIF) -> MediaKind.GIF
        MediaFiles.matchesMagic(file, MediaKind.STICKER) -> MediaKind.STICKER
        else -> null
    }

    fun title(rawName: String?, kind: MediaKind): String {
        val fileName = rawName.orEmpty().substringAfterLast('/').substringAfterLast('\\')
        val bareName = fileName.substringBeforeLast('.', fileName)
        val clean = bareName.replace(Regex("[\\p{Cc}\\p{Cf}]"), "")
            .trim().take(80).trim()
        return clean.ifBlank { if (kind == MediaKind.GIF) "Imported GIF" else "Imported sticker" }
    }
}
