package com.goreecloud.gifs.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import java.io.IOException

private val Context.favoritesDataStore by preferencesDataStore(name = "favorite_media")

class FavoritesStore(private val context: Context, private val mediaFiles: MediaFiles) {
    private val key = stringPreferencesKey("items")
    private val mutex = Mutex()

    val items: Flow<List<MediaItem>> = context.favoritesDataStore.data
        .catch { if (it is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw it }
        .map { prefs ->
            runCatching {
                val json = JSONArray(prefs[key] ?: "[]")
                (0 until json.length()).mapNotNull { n ->
                    json.optJSONObject(n)?.let(MediaItem::fromJson)
                }
            }.getOrDefault(emptyList())
        }

    /**
     * Import an explicitly selected local document as an offline favorite.
     * Roll back the private file if saving its index fails.
     */
    suspend fun importFromUri(uri: Uri): MediaItem = mutex.withLock {
        val current = items.first()
        val imported = mediaFiles.importFromUri(uri)
        try {
            context.favoritesDataStore.edit { prefs ->
                prefs[key] = JSONArray().apply {
                    (current + imported).forEach { put(it.toJson()) }
                }.toString()
            }
        } catch (error: Exception) {
            mediaFiles.removeFavorite(imported)
            throw error
        }
        imported
    }

    /** Returns true if the item is now favorited, false if removed. */
    suspend fun toggle(item: MediaItem): Boolean = mutex.withLock {
        val current = items.first()
        val existing = current.firstOrNull { it.id == item.id }
        if (existing != null) {
            context.favoritesDataStore.edit { prefs ->
                val remaining = current.filterNot { it.id == item.id }
                prefs[key] = JSONArray().apply {
                    remaining.forEach { put(it.toJson()) }
                }.toString()
            }
            mediaFiles.removeFavorite(existing)
            false
        } else {
            val offline = mediaFiles.favorite(item)
            context.favoritesDataStore.edit { prefs ->
                prefs[key] = JSONArray().apply {
                    (current + offline).forEach { put(it.toJson()) }
                }.toString()
            }
            true
        }
    }
}
