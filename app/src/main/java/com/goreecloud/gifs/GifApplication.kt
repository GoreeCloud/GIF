package com.goreecloud.gifs

import android.app.Application
import android.content.Context
import android.os.Build
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.goreecloud.gifs.data.PreviewCachePolicy
import com.goreecloud.gifs.data.mediaHttpClient

class GifApplication : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(context: Context): ImageLoader {
        // Preview thumbnails may redirect among public HTTPS hosts; neither
        // the image loader nor direct downloads can access private DNS targets.
        val previewClient = mediaHttpClient(allowHttpsRedirects = true)
        return ImageLoader.Builder(context)
            .diskCache { PreviewCachePolicy.create(context.cacheDir) }
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { previewClient }))
                if (Build.VERSION.SDK_INT >= 28) add(AnimatedImageDecoder.Factory())
                else add(GifDecoder.Factory())
            }
            .build()
    }
}
