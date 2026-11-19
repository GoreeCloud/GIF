package com.goreecloud.gifs.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID

/** Remote downloads are bounded, atomic, checked by magic bytes, and never executed. */
class MediaFiles(private val context: Context, private val client: OkHttpClient) {
    private val lock = Mutex()
    private val maxBytes = ImportPolicy.MAX_BYTES

    private fun safeName(item: MediaItem) =
        MessageDigest.getInstance("SHA-256")
            .digest(item.id.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) } + "." + item.extension

    private fun favoriteFile(item: MediaItem) = File(
        File(context.filesDir, "favorites"), safeName(item)
    )
    private fun shareFile(item: MediaItem) = File(
        File(context.cacheDir, "shares"), safeName(item)
    )

    suspend fun acquire(item: MediaItem): File = withContext(Dispatchers.IO) {
        lock.withLock {
            val saved = favoriteFile(item)
            if (saved.isFile && saved.length() > 0) return@withLock saved
            val cached = shareFile(item)
            if (cached.isFile && cached.length() > 0) return@withLock cached
            if (!SourcePolicy.isSafeHttps(item.url)) throw IOException("Unsafe media URL.")
            val dir = cached.parentFile ?: throw IOException("Missing cache directory")
            if (!dir.isDirectory && !dir.mkdirs()) throw IOException("Cannot make cache directory.")
            val temp = File.createTempFile("media-", ".part", dir)
            try {
                val request = Request.Builder().url(item.url)
                    .header("Accept", item.mimeType)
                    .header("User-Agent", "GoreeCloud-GIFs/0.1 (+https://github.com/GoreeCloud/GIF)")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Media unavailable (HTTP " + response.code + ").")
                    val body = response.body ?: throw IOException("Empty media download.")
                    if (body.contentLength() > maxBytes) throw IOException("Media exceeds 25 MB limit.")
                    body.byteStream().use { source ->
                        temp.outputStream().buffered().use { target ->
                            val buffer = ByteArray(8192)
                            var count = 0L
                            while (true) {
                                val read = source.read(buffer)
                                if (read == -1) break
                                count += read
                                if (count > maxBytes) throw IOException("Media exceeds 25 MB limit.")
                                target.write(buffer, 0, read)
                            }
                        }
                    }
                }
                if (!matchesMagic(temp, item.kind)) throw IOException("Unexpected image format.")
                if (!temp.renameTo(cached)) throw IOException("Cannot finalize downloaded image.")
            } finally {
                temp.delete()
            }
            cached
        }
    }

    /**
     * Explicitly user-selected SAF content; only GIF/PNG bytes are accepted.
     * Copy once into app-private storage. Never retain document-provider access.
     */
    suspend fun importFromUri(uri: Uri): MediaItem = withContext(Dispatchers.IO) {
        if (uri.scheme != "content" || uri.authority.isNullOrBlank()) {
            throw IOException("Only an Android document-provider image can be imported.")
        }
        val dir = File(context.filesDir, "favorites")
        if (!dir.isDirectory && !dir.mkdirs()) throw IOException("Cannot create favorites directory.")
        val temp = File.createTempFile("import-", ".part", dir)
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Cannot open the selected image.")
            input.use { source ->
                temp.outputStream().buffered().use { target ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val read = source.read(buffer)
                        if (read == -1) break
                        total += read
                        if (total > maxBytes) throw IOException("Image exceeds 25 MB limit.")
                        target.write(buffer, 0, read)
                    }
                }
            }
            val kind = ImportPolicy.detectKind(temp)
                ?: throw IOException("Select a valid GIF or PNG image.")
            val displayName = runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME),
                    null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getString(0) else null
                }
            }.getOrNull()
            val item = MediaItem(
                id = "local:" + UUID.randomUUID().toString(),
                title = ImportPolicy.title(displayName, kind),
                kind = kind,
                url = "",
                thumbnail = "",
                credit = "Imported from this device",
                sourceUrl = "",
                license = "User-supplied media; confirm sharing rights"
            )
            val destination = favoriteFile(item)
            if (destination.exists() || !temp.renameTo(destination)) {
                throw IOException("Cannot save the imported image.")
            }
            item.copy(localFile = destination.name)
        } finally {
            temp.delete()
        }
    }

    suspend fun favorite(item: MediaItem): MediaItem = withContext(Dispatchers.IO) {
        val source = acquire(item)
        val destination = favoriteFile(item)
        if (!destination.exists()) {
            val parent = destination.parentFile!!
            if (!parent.isDirectory && !parent.mkdirs()) throw IOException("Cannot create favorites folder.")
            val temporary = File.createTempFile("saved-", ".part", parent)
            try {
                source.inputStream().use { from ->
                    temporary.outputStream().use { to -> from.copyTo(to) }
                }
                if (!temporary.renameTo(destination)) throw IOException("Cannot save favorite.")
            } finally { temporary.delete() }
        }
        item.copy(localFile = destination.name)
    }

    suspend fun removeFavorite(item: MediaItem) = withContext(Dispatchers.IO) {
        favoriteFile(item).delete()
    }

    suspend fun shareUri(item: MediaItem): Uri = withContext(Dispatchers.IO) {
        val file = acquire(item)
        FileProvider.getUriForFile(context, "com.goreecloud.gifs.fileprovider", file)
    }

    suspend fun saveToPhone(item: MediaItem): Uri = withContext(Dispatchers.IO) {
        val file = acquire(item)
        val cleanTitle = item.title.replace(Regex("[^A-Za-z0-9 _-]"), "_")
            .trim().take(65).ifEmpty { "GoreeCloud" }
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, cleanTitle + "-" +
                safeName(item).take(10) + "." + item.extension)
            put(MediaStore.Images.Media.MIME_TYPE, item.mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/GoreeCloud GIFs")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values) ?: throw IOException("Cannot save to photo library.")
        try {
            val output = resolver.openOutputStream(uri) ?: throw IOException("MediaStore unavailable.")
            output.use { stream -> file.inputStream().use { it.copyTo(stream) } }
            val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
            uri
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    companion object {
        fun matchesMagic(file: File, kind: MediaKind): Boolean {
            if (!file.isFile || file.length() < 8) return false
            val bytes = ByteArray(8)
            file.inputStream().use { if (it.read(bytes) != 8) return false }
            return if (kind == MediaKind.GIF) {
                String(bytes.copyOfRange(0, 6), Charsets.US_ASCII) in
                    setOf("GIF87a", "GIF89a")
            } else bytes.contentEquals(byteArrayOf(
                0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
            ))
        }
    }
}
