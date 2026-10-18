package com.androbito.tilfaz

import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache

/** The posters and logos the app keeps, in memory and on disk. */
internal object ImageCaches {
    /** Empties both. The disk part is blocking file work, so call it off the main thread. */
    fun clear(loader: ImageLoader) = clear(loader.memoryCache, loader.diskCache)

    fun clear(memory: MemoryCache?, disk: DiskCache?) {
        memory?.clear()
        disk?.clear()
    }
}
