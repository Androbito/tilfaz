package com.androbito.tilfaz.core.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.Test

class RatingTest {
    @Test
    fun `provider rating out of ten is kept as is`() {
        assertThat(Rating.fromRaw(7.3)?.outOfTen).isEqualTo(7.3)
    }

    @Test
    fun `five based rating wins and is doubled`() {
        assertThat(Rating.fromRaw(value = 0.0, fiveBased = 3.5)?.outOfTen).isEqualTo(7.0)
    }

    @Test
    fun `percentage style rating is scaled down`() {
        assertThat(Rating.fromRaw(85.0)?.outOfTen).isEqualTo(8.5)
    }

    @Test
    fun `zero or missing rating means no rating`() {
        assertThat(Rating.fromRaw(0.0)).isNull()
        assertThat(Rating.fromRaw(null)).isNull()
    }

    @Test
    fun `badge shows the score with one decimal`() {
        assertThat(Rating(6.9).badge).isEqualTo("6.9")
        assertThat(Rating(7.0).badge).isEqualTo("7.0")
        assertThat(Rating(1.04).badge).isEqualTo("1.0")
    }

    @Test
    fun `badge rounds to the nearest tenth`() {
        assertThat(Rating(7.86).badge).isEqualTo("7.9")
    }

    @Test
    fun `a perfect or almost perfect score reads 10`() {
        assertThat(Rating(10.0).badge).isEqualTo("10")
        assertThat(Rating(9.96).badge).isEqualTo("10")
    }
}
