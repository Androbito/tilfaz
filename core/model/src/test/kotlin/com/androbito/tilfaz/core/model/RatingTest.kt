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
    fun `badge shows the integer part like the Smarters corner badge`() {
        assertThat(Rating(7.9).badge).isEqualTo("7")
        assertThat(Rating(10.0).badge).isEqualTo("10")
    }
}
