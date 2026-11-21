package com.goreecloud.gifs.data

import org.json.JSONArray
import org.json.JSONException
import java.io.IOException

/**
 * Fail closed on unreadable saved indexes. Silently returning an empty list
 * could cause the next write to replace the user's existing favorites.
 */
internal object SavedIndexCodec {
    const val MAX_ITEMS = 10_000
    private const val ERROR = "Saved gallery index is unreadable. Existing files were not changed."

    fun decode(raw: String): List<MediaItem> {
        try {
            val records = JSONArray(raw)
            if (records.length() > MAX_ITEMS) throw IOException(ERROR)
            return (0 until records.length()).map { index ->
                val record = records.optJSONObject(index) ?: throw IOException(ERROR)
                MediaItem.fromJson(record) ?: throw IOException(ERROR)
            }
        } catch (error: JSONException) {
            throw IOException(ERROR, error)
        }
    }

    fun encode(items: List<MediaItem>): String {
        if (items.size > MAX_ITEMS) {
            throw IOException("Saved gallery is full. Existing files were not changed.")
        }
        return JSONArray().apply { items.forEach { put(it.toJson()) } }.toString()
    }
}
