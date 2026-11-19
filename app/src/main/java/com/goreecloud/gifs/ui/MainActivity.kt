package com.goreecloud.gifs.ui

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import coil3.compose.AsyncImage
import com.goreecloud.gifs.data.MediaItem
import com.goreecloud.gifs.data.MediaKind
import com.goreecloud.gifs.data.SourcePolicy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: GalleryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GoreeCloudTheme {
                GalleryScreen(
                    viewModel = vm,
                    onShare = { item -> send(item) },
                    onSource = { url -> openSource(url) }
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
    onSource: (String) -> Unit
) {
    val results by viewModel.results.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.message.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var type by rememberSaveable { mutableStateOf(MediaKind.GIF.name) }
    var query by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf<MediaItem?>(null) }
    var busyId by remember { mutableStateOf<String?>(null) }
    val kind = MediaKind.valueOf(type)
    val uiScope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(query, type, tab) {
        if (tab == 0) {
            delay(if (query.isBlank()) 0 else 400)
            viewModel.search(query, kind)
        }
    }
    LaunchedEffect(error) {
        if (!error.isNullOrBlank()) {
            snackbar.showSnackbar(error!!)
            viewModel.clearMessage()
        }
    }

    val shown = if (tab == 0) results else favorites.filter {
        query.isBlank() || it.title.contains(query, ignoreCase = true)
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
                        Text("Find it. Save it. Send it.",
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
                    value = query, onValueChange = { query = it.take(100) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    singleLine = true,
                    placeholder = { Text(if (tab == 1) "Search saved items" else "Search GIFs and stickers") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.search(query, kind) })
                )
                if (tab == 0) {
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = kind == MediaKind.GIF,
                            onClick = { type = MediaKind.GIF.name },
                            label = { Text("GIFs") },
                            leadingIcon = { Icon(Icons.Default.Movie, null, Modifier.size(18.dp)) }
                        )
                        FilterChip(
                            selected = kind == MediaKind.STICKER,
                            onClick = { type = MediaKind.STICKER.name },
                            label = { Text("Stickers") },
                            leadingIcon = { Icon(Icons.Default.EmojiEmotions, null, Modifier.size(18.dp)) }
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
                        Icon(if (tab == 1) Icons.Default.FavoriteBorder else Icons.Default.ImageSearch,
                            contentDescription = null, modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        Text(if (tab == 1) "Nothing saved yet" else "No results found",
                            style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (tab == 1) "Tap the heart on a GIF or sticker to keep it offline."
                            else "Try a different phrase or switch media type.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                            onShare = { onShare(item) }
                        )
                    }
                }
            }
        }
    }
    selected?.let { item ->
        ModalBottomSheet(onDismissRequest = { selected = null },
            containerColor = MaterialTheme.colorScheme.surface) {
            MediaDetail(
                item = item,
                isFavorite = item.id in favoriteIds,
                busy = busyId == item.id,
                onFavorite = { favorite(item) },
                onShare = { onShare(item) },
                onDownload = { download(item) },
                onSource = { onSource(item.sourceUrl) }
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
    onShare: () -> Unit
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
                    Icon(Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Share " + item.title)
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
    onSource: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding()
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MediaPreview(item, Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 280.dp)
            .clip(RoundedCornerShape(18.dp)), animate = true)
        Spacer(Modifier.height(14.dp))
        Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(item.license, style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary)
        Text(item.credit, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3,
            overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onShare, enabled = !busy, modifier = Modifier.weight(1f)) {
                Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp)); Text("Share")
            }
            Button(onClick = onDownload, enabled = !busy, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Download, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp)); Text("Download")
            }
        }
        TextButton(onClick = onFavorite, enabled = !busy) {
            Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (isFavorite) "Remove from favorites" else "Save offline favorite")
        }
        TextButton(onClick = onSource) {
            Icon(Icons.Default.OpenInNew, null, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp)); Text("View source and licensing")
        }
    }
}
