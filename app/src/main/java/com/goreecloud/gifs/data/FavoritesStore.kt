package com.goreecloud.gifs.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val Context.favoritesDataStore by preferencesDataStore(name = "favorite_media")

class FavoritesStore(private val context: Context, private val mediaFiles: MediaFiles) {
    private val key = stringPreferencesKey("items")
    private val mutex = Mutex()

    val items: Flow<List<MediaItem>> = context.favoritesDataStore.data
        .map { prefs -> SavedIndexCodec.decode(prefs[key] ?: "[]") }

    /**
     * Import an explicitly selected local document as an offline favorite.
     * Roll back the private file if saving its index fails.
     */
    suspend fun importFromUri(uri: Uri): MediaItem = mutex.withLock {
        val current = items.first()
        val imported = mediaFiles.importFromUri(uri)
        try {
            context.favoritesDataStore.edit { prefs ->
                prefs[key] = SavedIndexCodec.encode(current + imported)
            }
        } catch (error: Exception) {
            // Do not mask the index write error if removing the staged image fails.
            try { mediaFiles.removeFavorite(imported) } catch (_: Exception) { }
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
                prefs[key] = SavedIndexCodec.encode(remaining)
            }
            mediaFiles.removeFavorite(existing)
            false
        } else {
            val previouslyPresent = mediaFiles.hasFavoriteFile(item)
            val offline = mediaFiles.favorite(item)
            try {
                context.favoritesDataStore.edit { prefs ->
                    prefs[key] = SavedIndexCodec.encode(current + offline)
                }
            } catch (error: Exception) {
                // Never delete an older, unindexed file during rollback.
                if (!previouslyPresent) {
                    try { mediaFiles.removeFavorite(offline) } catch (_: Exception) { }
                }
                throw error
            }
            true
        }
    }
}
