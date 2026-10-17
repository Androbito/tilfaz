package com.androbito.tilfaz

import android.app.ActivityManager
import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import okhttp3.OkHttpClient

@HiltAndroidApp
class TilfazApplication : Application(), SingletonImageLoader.Factory {

    @Inject lateinit var okHttpClient: OkHttpClient

    /**
     * Only links are stored with the catalog, so every poster is a download kept in a cache. The cache is sized to the device:
     * a twentieth of its free storage and an eighth of the memory the system lets the app use, both within fixed bounds, so
     * it never fills a TV's small storage or squeezes the player.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val memoryClassMb = (getSystemService(ACTIVITY_SERVICE) as ActivityManager).memoryClass
        val directory = cacheDir.resolve("images").apply { mkdirs() }
        return ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient })) }
            .memoryCache { MemoryCache.Builder().maxSizeBytes(ImageCacheLimits.memoryBytes(memoryClassMb)).build() }
            .diskCache { DiskCache.Builder().directory(directory).maxSizeBytes(ImageCacheLimits.diskBytes(directory.usableSpace)).build() }
            .crossfade(true)
            .build()
    }
}
