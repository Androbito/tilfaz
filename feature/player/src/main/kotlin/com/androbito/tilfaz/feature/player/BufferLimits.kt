package com.androbito.tilfaz.feature.player

/**
 * How many bytes the player may hold ahead of the playhead. The player buffers into the app's own memory, and a high bitrate
 * film buffered by time alone (50 seconds of a 4K file) can outgrow what a small TV gives an app, so the amount is also capped
 * by size: a quarter of the app's memory, never less than 16 MB nor more than 64 MB.
 */
internal object BufferLimits {
    private const val MEGABYTE = 1024 * 1024
    private const val MIN_MB = 16
    private const val MAX_MB = 64

    fun targetBytes(memoryClassMb: Int): Int = (memoryClassMb / 4).coerceIn(MIN_MB, MAX_MB) * MEGABYTE
}
