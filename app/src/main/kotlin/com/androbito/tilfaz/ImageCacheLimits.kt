package com.androbito.tilfaz

/**
 * How much posters and logos may be kept. Only the links are stored with the catalog; the pictures are a cache that can be
 * refilled at any time, so it must never crowd out the film being watched or fill a TV's small storage.
 */
internal object ImageCacheLimits {
    private const val MEGABYTE = 1024L * 1024
    private const val MIN_DISK_MB = 32L
    private const val MAX_DISK_MB = 150L
    private const val DISK_SHARE_OF_FREE = 20
    private const val MIN_MEMORY_MB = 8L
    private const val MAX_MEMORY_MB = 48L
    private const val MEMORY_SHARE_OF_APP = 8

    /** A twentieth of the free space, between 32 and 150 MB. */
    fun diskBytes(freeBytes: Long): Long = (freeBytes / DISK_SHARE_OF_FREE).coerceIn(MIN_DISK_MB * MEGABYTE, MAX_DISK_MB * MEGABYTE)

    /** An eighth of what the system lets the app use, between 8 and 48 MB. */
    fun memoryBytes(memoryClassMb: Int): Long = (memoryClassMb.toLong() / MEMORY_SHARE_OF_APP).coerceIn(MIN_MEMORY_MB, MAX_MEMORY_MB) * MEGABYTE
}
