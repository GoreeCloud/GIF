package com.goreecloud.gifs.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import coil3.compose.AsyncImage
import com.goreecloud.gifs.data.MediaAttribution
import com.goreecloud.gifs.data.MediaItem
import com.goreecloud.gifs.data.MediaKind
import com.goreecloud.gifs.data.SavedGallery
import com.goreecloud.gifs.data.SavedKind
import com.goreecloud.gifs.data.PickerPolicy
import com.goreecloud.gifs.data.SourcePolicy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: GalleryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val requestedMime = if (intent?.action == Intent.ACTION_GET_CONTENT) intent.type?.lowercase() else null
        if (intent?.action == Intent.ACTION_GET_CONTENT &&
            !PickerPolicy.isSupported(requestedMime)) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        setContent {
            GoreeCloudTheme {
                GalleryScreen(
                    viewModel = vm,
                    onShare = { item -> send(item) },
                    onSource = { url -> openSource(url) },
                    onPick = if (requestedMime != null) {
                        { item: MediaItem -> pick(item, requestedMime) }
                    } else null,
                    requestedMime = requestedMime
                )
            }
        }
    }

    private fun send(item: MediaItem) {
        // Open the system share sheet only when a safe content:// URI is ready.
        val scope = vm
        lifecycleScope.launch {
            try {
                val uri = scope.shareUri(item)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = item.mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = ClipData.newUri(contentResolver, item.title, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "Share " + item.title))
            } catch (error: Exception) {
                vm.showMessage(error.message ?: "Could not share this image.")
            }
        }
    }

    private fun pick(item: MediaItem, requestedMime: String) {
        if (!PickerPolicy.accepts(requestedMime, item)) {
            vm.showMessage("This app requested a different image format.")
            return
        }
        lifecycleScope.launch {
            try {
                val uri = vm.shareUri(item)
                val result = Intent().apply {
                    setDataAndType(uri, item.mimeType)
                    clipData = ClipData.newUri(contentResolver, item.title, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                setResult(RESULT_OK, result)
                finish()
            } catch (error: Exception) {
                vm.showMessage(error.message ?: "Unable to provide this image.")
            }
        }
    }

    private fun openSource(url: String) {
        if (!SourcePolicy.isSafeHttps(url)) return
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryScreen(
    viewModel: GalleryViewModel,
    onShare: (MediaItem) -> Unit,
    onSource: (String) -> Unit,
    onPick: ((MediaItem) -> Unit)?,
    requestedMime: String?
) {
    val results by viewModel.results.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val loadingMore by viewModel.loadingMore.collectAsStateWithLifecycle()
    val hasMore by viewModel.hasMore.collectAsStateWithLifecycle()
    val searchError by viewModel.searchError.collectAsStateWithLifecycle()
    val error by viewModel.message.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var type by rememberSaveable {
        mutableStateOf(PickerPolicy.defaultKind(requestedMime).name)
    }
    var exploreQuery by rememberSaveable { mutableStateOf("") }
    var savedQuery by rememberSaveable { mutableStateOf("") }
    var savedFilter by rememberSaveable { mutableStateOf(SavedKind.ALL.name) }
    val query = if (tab == 0) exploreQuery else savedQuery
    var selected by remember { mutableStateOf<MediaItem?>(null) }
    var busyId by remember { mutableStateOf<String?>(null) }
    val kind = MediaKind.valueOf(type)
    val uiScope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(exploreQuery, type, tab) {
        if (tab == 0) {
            delay(if (exploreQuery.isBlank()) 0 else 400)
            viewModel.search(exploreQuery, kind)
        }
    }
    LaunchedEffect(error) {
        if (!error.isNullOrBlank()) {
            snackbar.showSnackbar(error!!)
            viewModel.clearMessage()
        }
    }

    val shown = if (tab == 0) {
        results.filter { requestedMime == null || PickerPolicy.accepts(requestedMime, it) }
    } else {
        SavedGallery.filter(favorites, savedQuery, SavedKind.valueOf(savedFilter), requestedMime)
    }
    val favoriteIds = favorites.map { it.id }.toSet()

    fun favorite(item: MediaItem) {
        if (busyId != null) return
        busyId = item.id
        uiScope.launch {
            try {
                val nowSaved = viewModel.toggleFavorite(item)
                snackbar.showSnackbar(if (nowSaved) "Saved for offline use" else "Removed from favorites")
            } catch (e: Exception) {
                snackbar.showSnackbar(e.message ?: "Could not update favorites.")
            } finally { busyId = null }
        }
    }
    fun copyAttribution(item: MediaItem) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard == null) {
            uiScope.launch { snackbar.showSnackbar("Clipboard unavailable") }
            return
        }
        clipboard.setPrimaryClip(ClipData.newPlainText(
            "Media attribution", MediaAttribution.text(item)
        ))
        uiScope.launch { snackbar.showSnackbar("Creator, license and source copied") }
    }
    fun download(item: MediaItem) {
        if (busyId != null) return
        busyId = item.id
        uiScope.launch {
            try {
                viewModel.download(item)
                snackbar.showSnackbar("Saved to Pictures / GoreeCloud GIFs")
            } catch (e: Exception) {
                snackbar.showSnackbar(e.message ?: "Could not save this image.")
            } finally { busyId = null }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Explore, contentDescription = null) },
                    label = { Text("Explore") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                    label = { Text("Saved (" + favorites.size + ")") }
                )
            }
        }
    ) { insets ->
        Column(
            modifier = Modifier.fillMaxSize().padding(insets)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(50.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.GifBox, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(30.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("GoreeCloud GIF's", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                        Text(if (onPick != null) "Choose media for another app."
                             else "Find it. Save it. Send it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = {
                        viewModel.showMessage("GIFs: Openverse. Stickers: OpenMoji. Check each item's license before reuse.")
                    }) {
                        Icon(Icons.Default.Info, contentDescription = "About sources and licenses")
                    }
                }
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        if (tab == 0) exploreQuery = it.take(100)
                        else savedQuery = it.take(100)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    singleLine = true,
                    placeholder = { Text(if (tab == 1) "Search saved items" else "Search GIFs and stickers") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = {
                                if (tab == 0) exploreQuery = "" else savedQuery = ""
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (tab == 0) viewModel.search(exploreQuery, kind)
                        focusManager.clearFocus()
                    })
                )
                if (tab == 0) {
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = kind == MediaKind.GIF,
                            onClick = { type = MediaKind.GIF.name },
                            enabled = requestedMime == null || requestedMime == "image/*" ||
                                requestedMime == "image/gif",
                            label = { Text("GIFs") },
                            leadingIcon = { Icon(Icons.Default.Movie, null, Modifier.size(18.dp)) }
                        )
                        FilterChip(
                            selected = kind == MediaKind.STICKER,
                            onClick = { type = MediaKind.STICKER.name },
                            enabled = requestedMime == null || requestedMime == "image/*" ||
                                requestedMime == "image/png",
                            label = { Text("Stickers") },
                            leadingIcon = { Icon(Icons.Default.EmojiEmotions, null, Modifier.size(18.dp)) }
                        )
                    }
                } else {
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = savedFilter == SavedKind.ALL.name,
                            onClick = { savedFilter = SavedKind.ALL.name },
                            label = { Text("All") }
                        )
                        FilterChip(
                            selected = savedFilter == SavedKind.GIF.name,
                            onClick = { savedFilter = SavedKind.GIF.name },
                            enabled = requestedMime == null || requestedMime == "image/*" ||
                                requestedMime == "image/gif",
                            label = { Text("GIFs") }
                        )
                        FilterChip(
                            selected = savedFilter == SavedKind.STICKER.name,
                            onClick = { savedFilter = SavedKind.STICKER.name },
                            enabled = requestedMime == null || requestedMime == "image/*" ||
                                requestedMime == "image/png",
                            label = { Text("Stickers") }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (tab == 1) "Offline favorites"
                        else if (kind == MediaKind.GIF) "Animated discoveries" else "OpenMoji stickers",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.weight(1f))
                    Text(shown.size.toString() + " items", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (loading && tab == 0) {
                LinearProgressIndicator(Modifier.fillMaxWidth().semantics {
                    contentDescription = "Loading GIFs or stickers"
                })
            }
            if (shown.isEmpty() && !loading) {
                Box(Modifier.fillMaxSize().padding(30.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            if (tab == 1) Icons.Default.FavoriteBorder
                            else if (searchError != null) Icons.Default.WifiOff
                            else Icons.Default.ImageSearch,
                            contentDescription = null, modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (tab == 1 && favorites.isEmpty()) "Nothing saved yet"
                            else if (tab == 1) "No matching saved items"
                            else if (searchError != null) "Search unavailable"
                            else "No results found",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            if (tab == 1 && favorites.isEmpty())
                                "Tap the heart on a GIF or sticker to keep it offline."
                            else if (tab == 1) "Try another search or switch the saved media filter."
                            else searchError ?: "Try a different phrase or switch media type.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (tab == 0 && searchError != null) {
                            Spacer(Modifier.height(14.dp))
                            Button(onClick = { viewModel.search(query, kind) }) {
                                Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Try again")
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(shown, key = { it.id }) { item ->
                        MediaTile(
                            item = item,
                            isFavorite = item.id in favoriteIds,
                            enabled = busyId == null,
                            onSelect = { selected = item },
                            onFavorite = { favorite(item) },
                            onShare = { (onPick ?: onShare)(item) },
                            isPicker = onPick != null
                        )
                    }
                    if (tab == 0 && kind == MediaKind.GIF && hasMore) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(Modifier.fillMaxWidth().padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center) {
                                OutlinedButton(
                                    onClick = { viewModel.loadMore() },
                                    enabled = !loadingMore
                                ) {
                                    if (loadingMore) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (loadingMore) "Loading more…" else "Load more GIFs")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    selected?.let { item ->
        ModalBottomSheet(
            onDismissRequest = { selected = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            MediaDetail(
                item = item,
                isFavorite = item.id in favoriteIds,
                busy = busyId == item.id,
                onFavorite = { favorite(item) },
                onShare = { (onPick ?: onShare)(item) },
                onDownload = { download(item) },
                isPicker = onPick != null,
                onSource = { onSource(item.sourceUrl) },
                onCopyAttribution = { copyAttribution(item) }
            )
        }
    }
}

@Composable
private fun MediaPreview(item: MediaItem, modifier: Modifier = Modifier, animate: Boolean = false) {
    val context = LocalContext.current
    val saved = item.localFile?.let {
        java.io.File(context.filesDir, "favorites/" + it)
    }?.takeIf { it.isFile }
    AsyncImage(
        model = saved ?: if (animate && item.kind == MediaKind.GIF) item.url else item.thumbnail,
        contentDescription = item.title,
        contentScale = ContentScale.Fit,
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

@Composable
private fun MediaTile(
    item: MediaItem,
    isFavorite: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    isPicker: Boolean
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .clickable(onClick = onSelect)
            ) {
                MediaPreview(item, Modifier.fillMaxSize())
            }
            Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 12.dp, end = 10.dp, top = 10.dp))
            Row(Modifier.fillMaxWidth().padding(start = 3.dp, end = 3.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onFavorite, enabled = enabled,
                    modifier = Modifier.size(44.dp)) {
                    Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove favorite" else "Save favorite",
                        tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onShare, enabled = enabled,
                    modifier = Modifier.size(44.dp)) {
                    Icon(
                        if (isPicker) Icons.Default.CheckCircle
                        else Icons.AutoMirrored.Filled.Send,
                        contentDescription = (if (isPicker) "Choose " else "Share ") + item.title
                    )
                }
                IconButton(onClick = onSelect, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "More options for " + item.title)
                }
            }
        }
    }
}

@Composable
private fun MediaDetail(
    item: MediaItem,
    isFavorite: Boolean,
    busy: Boolean,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit,
    isPicker: Boolean,
    onSource: () -> Unit,
    onCopyAttribution: () -> Unit
) {
    // Put primary actions first, before a potentially tall animated preview.
    // Scrollable content keeps secondary actions reachable on small screens,
    // with large accessibility text and in landscape orientation.
    Column(
        modifier = Modifier.fillMaxWidth()
            .heightIn(max = 640.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            item.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onShare, enabled = !busy, modifier = Modifier.weight(1f)) {
                Icon(
                    if (isPicker) Icons.Default.CheckCircle
                    else Icons.AutoMirrored.Filled.Send,
                    null, Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(if (isPicker) "Use in app" else "Share")
            }
            Button(onClick = onDownload, enabled = !busy, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Download, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp)); Text("Download")
            }
        }
        Spacer(Modifier.height(16.dp))
        MediaPreview(
            item,
            Modifier.fillMaxWidth().heightIn(min = 140.dp, max = 185.dp)
                .clip(RoundedCornerShape(18.dp)),
            animate = true
        )
        Spacer(Modifier.height(12.dp))
        Text(
            item.license,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            item.credit,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        TextButton(onClick = onFavorite, enabled = !busy) {
            Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (isFavorite) "Remove from favorites" else "Save offline favorite")
        }
        TextButton(onClick = onCopyAttribution) {
            Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp)); Text("Copy creator and license credit")
        }
        if (SourcePolicy.isSafeHttps(item.sourceUrl)) {
            TextButton(onClick = onSource) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp)); Text("View source and licensing")
            }
        }
    }
}
