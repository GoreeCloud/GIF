package com.goreecloud.gifs.data

import java.security.MessageDigest

/**
 * Canonical app-private filename; no persisted string may select an arbitrary path.
 * SHA-256 filenames are identifiers, not claims of content authenticity.
 */
object SavedFileNames {
    fun expected(id: String, kind: MediaKind): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(id.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return digest + if (kind == MediaKind.GIF) ".gif" else ".png"
    }

    fun matches(id: String, kind: MediaKind, actual: String): Boolean =
        actual == expected(id, kind)
}
