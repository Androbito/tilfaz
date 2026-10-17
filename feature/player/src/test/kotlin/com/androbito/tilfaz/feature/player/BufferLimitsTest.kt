package com.androbito.tilfaz.feature.player

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class BufferLimitsTest {
    private val mb = 1024 * 1024

    @Test
    fun `the buffer may use a quarter of the app's memory`() {
        assertThat(BufferLimits.targetBytes(memoryClassMb = 192)).isEqualTo(48 * mb)
        assertThat(BufferLimits.targetBytes(memoryClassMb = 128)).isEqualTo(32 * mb)
    }

    @Test
    fun `a small device still gets a usable buffer`() {
        assertThat(BufferLimits.targetBytes(memoryClassMb = 48)).isEqualTo(16 * mb)
    }

    @Test
    fun `a big device does not buffer without limit`() {
        assertThat(BufferLimits.targetBytes(memoryClassMb = 512)).isEqualTo(64 * mb)
    }
}
