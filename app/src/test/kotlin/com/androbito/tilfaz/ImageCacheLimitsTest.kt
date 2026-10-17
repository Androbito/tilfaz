package com.androbito.tilfaz

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class ImageCacheLimitsTest {
    private val mb = 1024L * 1024

    @Test
    fun `the disk cache takes a twentieth of the free space`() {
        assertThat(ImageCacheLimits.diskBytes(freeBytes = 2_000 * mb)).isEqualTo(100 * mb)
    }

    @Test
    fun `a nearly full TV gets a small cache but never none`() {
        assertThat(ImageCacheLimits.diskBytes(freeBytes = 1_100 * mb)).isEqualTo(55 * mb)
        assertThat(ImageCacheLimits.diskBytes(freeBytes = 200 * mb)).isEqualTo(32 * mb)
        assertThat(ImageCacheLimits.diskBytes(freeBytes = 0)).isEqualTo(32 * mb)
    }

    @Test
    fun `a roomy device is capped, posters do not need gigabytes`() {
        assertThat(ImageCacheLimits.diskBytes(freeBytes = 50_000 * mb)).isEqualTo(150 * mb)
    }

    @Test
    fun `the memory cache is a small share of the app's memory`() {
        assertThat(ImageCacheLimits.memoryBytes(memoryClassMb = 192)).isEqualTo(24 * mb)
        assertThat(ImageCacheLimits.memoryBytes(memoryClassMb = 64)).isEqualTo(8 * mb)
    }

    @Test
    fun `the memory cache stays within sensible bounds`() {
        assertThat(ImageCacheLimits.memoryBytes(memoryClassMb = 16)).isEqualTo(8 * mb)
        assertThat(ImageCacheLimits.memoryBytes(memoryClassMb = 1_024)).isEqualTo(48 * mb)
    }
}
