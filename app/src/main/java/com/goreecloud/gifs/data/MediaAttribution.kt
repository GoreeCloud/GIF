package com.goreecloud.gifs.data

/**
 * Keeps creator/license details alongside media shared or downloaded elsewhere.
 * Attribution is informative, not a grant of rights for downstream redistribution.
 */
object MediaAttribution {
    fun text(item: MediaItem): String = listOf(
        item.title.trim().ifBlank { "Untitled media" },
        item.credit.trim().ifBlank { "Creator not supplied" },
        "License: " + item.license.trim().ifBlank { "See source" },
        "Source: " + item.sourceUrl.takeIf(SourcePolicy::isSafeHttps).orEmpty()
    ).filterNot { it == "Source: " }.joinToString("\n")
}
