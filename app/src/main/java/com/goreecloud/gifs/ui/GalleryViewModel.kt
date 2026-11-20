package com.goreecloud.gifs.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.goreecloud.gifs.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
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
    val loadingMore = MutableStateFlow(false)
    val hasMore = MutableStateFlow(false)
    val searchError = MutableStateFlow<String?>(null)
    val message = MutableStateFlow<String?>(null)
    // Last successfully imported media navigates users to their private Saved gallery.
    val lastImportedId = MutableStateFlow<String?>(null)
    // Import only after the person confirms a share-target request.
    val pendingSharedUri = MutableStateFlow<Uri?>(null)

    fun offerSharedUri(uri: Uri) { pendingSharedUri.value = uri }
    fun declineSharedUri() { pendingSharedUri.value = null }

    suspend fun acceptSharedUri(uri: Uri): MediaItem {
        // A different inbound share may have superseded the current offer.
        require(pendingSharedUri.value == uri) { "Shared image request changed." }
        val imported = importFromUri(uri)
        if (pendingSharedUri.value == uri) pendingSharedUri.value = null
        return imported
    }

    private var searchJob: Job? = null
    private var loadMoreJob: Job? = null
    private var activeQuery = "reaction"
    private var activeKind = MediaKind.GIF
    private var currentPage = 0

    init {
        viewModelScope.launch { storage.items.collect { favorites.value = it } }
    }

    fun search(query: String, kind: MediaKind) {
        val normalizedQuery = query.trim().ifBlank { "reaction" }
        activeQuery = normalizedQuery
        activeKind = kind
        currentPage = 0
        searchJob?.cancel()
        loadMoreJob?.cancel()
        searchJob = viewModelScope.launch {
            loading.value = true
            loadingMore.value = false
            hasMore.value = false
            searchError.value = null
            message.value = null
            try {
                when (kind) {
                    MediaKind.GIF -> {
                        val page = gifSource.searchGifs(normalizedQuery)
                        results.value = page.items
                        currentPage = page.page
                        hasMore.value = page.hasMore
                    }
                    MediaKind.STICKER -> results.value = stickers.search(query)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                results.value = emptyList()
                searchError.value = e.message ?: "Unable to search. Check your connection."
            } finally {
                loading.value = false
            }
        }
    }

    fun loadMore() {
        if (activeKind != MediaKind.GIF || loading.value || loadingMore.value || !hasMore.value) return
        val query = activeQuery
        val nextPage = currentPage + 1
        loadMoreJob = viewModelScope.launch {
            loadingMore.value = true
            try {
                val next = gifSource.searchGifs(query, nextPage)
                results.value = (results.value + next.items).distinctBy { it.id }
                currentPage = next.page
                hasMore.value = next.hasMore
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message.value = e.message ?: "Unable to load more GIFs."
            } finally {
                loadingMore.value = false
            }
        }
    }

    suspend fun importFromUri(uri: Uri): MediaItem {
        val item = storage.importFromUri(uri)
        lastImportedId.value = item.id
        return item
    }
    suspend fun toggleFavorite(item: MediaItem): Boolean = storage.toggle(item)
    suspend fun shareUri(item: MediaItem) = files.shareUri(item)
    suspend fun download(item: MediaItem) = files.saveToPhone(item)
    fun clearMessage() { message.value = null }
    fun showMessage(text: String) { message.value = text }
}
