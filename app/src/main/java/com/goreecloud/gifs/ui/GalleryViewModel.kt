package com.goreecloud.gifs.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.goreecloud.gifs.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GalleryViewModel(app: Application) : AndroidViewModel(app) {
    private val http = mediaHttpClient()
    private val gifSource = OpenverseClient(http)
    private val stickers = OpenMojiCatalog(app)
    private val files = MediaFiles(app, http)
    private val storage = FavoritesStore(app, files)
    val favorites = MutableStateFlow<List<MediaItem>>(emptyList())
    val results = MutableStateFlow<List<MediaItem>>(emptyList())
    val loading = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)
    private var searchJob: Job? = null

    init {
        viewModelScope.launch { storage.items.collect { favorites.value = it } }
    }

    fun search(query: String, kind: MediaKind) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            loading.value = true
            message.value = null
            try {
                results.value = when(kind) {
                    MediaKind.GIF -> gifSource.searchGifs(query.ifBlank { "reaction" })
                    MediaKind.STICKER -> stickers.search(query)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                results.value = emptyList()
                message.value = e.message ?: "Unable to search at the moment."
            } finally {
                loading.value = false
            }
        }
    }

    suspend fun toggleFavorite(item: MediaItem): Boolean = storage.toggle(item)
    suspend fun shareUri(item: MediaItem) = files.shareUri(item)
    suspend fun download(item: MediaItem) = files.saveToPhone(item)

    fun clearMessage() { message.value = null }
    fun showMessage(text: String) { message.value = text }
}
