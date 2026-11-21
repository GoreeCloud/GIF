package com.goreecloud.gifs.interopfixture

import android.app.Activity
import android.content.ClipData
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.io.IOException

/**
 * CI-only external caller. It cannot use GoreeCloud's in-process APIs or files.
 * A successful result proves an actual Android cross-UID URI grant.
 */
class InteropActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "GoreeCloud interoperability fixture"

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 44, 32, 24)
        }
        layout.addView(TextView(this).apply {
            text = "GoreeCloud interoperability fixture"
            textSize = 22f
        })
        layout.addView(Button(this).apply {
            text = "Send PNG to GoreeCloud"
            setOnClickListener { sendExternalPng() }
        })
        layout.addView(Button(this).apply {
            text = "Request PNG from GoreeCloud"
            setOnClickListener { requestPngFromGoreeCloud() }
        })
        status = TextView(this).apply {
            text = "Ready for cross-app Android GIF acceptance"
            textSize = 16f
        }
        layout.addView(status)
        setContentView(layout)
    }

    private fun sendExternalPng() {
        val source = FixtureMediaProvider.IMAGE_URI
        val send = Intent(Intent.ACTION_SEND).apply {
            component = GOREECLOUD_ACTIVITY
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, source)
            clipData = ClipData.newUri(contentResolver, "QA PNG", source)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        status.text = "Sending user-controlled PNG"
        startActivity(send)
    }

    @Suppress("DEPRECATION")
    private fun requestPngFromGoreeCloud() {
        val request = Intent(Intent.ACTION_GET_CONTENT).apply {
            component = GOREECLOUD_ACTIVITY
            type = "image/png"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        writeResult("REQUESTING")
        startActivityForResult(request, PICK_PNG)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PICK_PNG) return
        if (resultCode != RESULT_OK) {
            writeResult("FAIL result canceled")
            return
        }
        val uri = data?.data
        if (uri == null || uri.scheme != "content" ||
            uri.authority != "com.goreecloud.gifs.fileprovider"
        ) {
            writeResult("FAIL unexpected returned URI authority")
            return
        }

        try {
            val mime = contentResolver.getType(uri)
            if (mime != "image/png") {
                throw IOException("Unexpected MIME: " + mime)
            }
            val prefix = ByteArray(PNG_SIGNATURE.size)
            val total = contentResolver.openInputStream(uri)?.use { input ->
                var read = 0
                while (read < prefix.size) {
                    val count = input.read(prefix, read, prefix.size - read)
                    if (count < 0) break
                    read += count
                }
                read
            } ?: throw IOException("No cross-UID read grant")
            if (total != prefix.size || !prefix.contentEquals(PNG_SIGNATURE)) {
                throw IOException("PNG signature mismatch")
            }
            writeResult("PASS PNG GRANT mime=image/png")
        } catch (error: Exception) {
            // Do not log raw content URIs, file bytes or other app data.
            writeResult("FAIL " + error.javaClass.simpleName)
        }
    }

    private fun writeResult(value: String) {
        openFileOutput("interop-result.txt", MODE_PRIVATE)
            .bufferedWriter().use { it.write(value.take(120)) }
        status.text = value
    }

    companion object {
        private const val PICK_PNG = 301
        private val GOREECLOUD_ACTIVITY =
            ComponentName("com.goreecloud.gifs", "com.goreecloud.gifs.ui.MainActivity")
        private val PNG_SIGNATURE = byteArrayOf(
            0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
        )
    }
}
