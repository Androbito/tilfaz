package com.androbito.tilfaz

import assertk.assertThat
import assertk.assertions.isEqualTo
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.junit.Test

class ImageCachesTest {
    private class FakeDisk : DiskCache {
        var cleared = 0
        override val size = 0L
        override val maxSize = 0L
        override val directory: Path = "/tmp/none".toPath()
        override val fileSystem: FileSystem = FileSystem.SYSTEM

        override fun openSnapshot(key: String): DiskCache.Snapshot? = null

        override fun openEditor(key: String): DiskCache.Editor? = null

        override fun remove(key: String) = false

        override fun clear() {
            cleared++
        }

        override fun shutdown() = Unit
    }

    private class FakeMemory : MemoryCache {
        var cleared = 0
        override val size = 0L
        override val initialMaxSize = 0L
        override var maxSize = 0L
        override val keys: Set<MemoryCache.Key> = emptySet()

        override fun get(key: MemoryCache.Key): MemoryCache.Value? = null

        override fun set(key: MemoryCache.Key, value: MemoryCache.Value) = Unit

        override fun remove(key: MemoryCache.Key) = false

        override fun trimToSize(size: Long) = Unit

        override fun clear() {
            cleared++
        }
    }

    @Test
    fun `clearing empties the posters held in memory and on disk`() {
        val memory = FakeMemory()
        val disk = FakeDisk()

        ImageCaches.clear(memory, disk)

        assertThat(memory.cleared).isEqualTo(1)
        assertThat(disk.cleared).isEqualTo(1)
    }

    @Test
    fun `a loader without a disk cache is fine`() {
        val memory = FakeMemory()

        ImageCaches.clear(memory, null)

        assertThat(memory.cleared).isEqualTo(1)
    }
}
