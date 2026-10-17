package com.androbito.tilfaz.core.domain.search

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotSameInstanceAs
import assertk.assertions.isSameInstanceAs
import org.junit.Test

class TitleSearchCacheTest {
    private val cache = TitleSearchCache<String> { it }

    @Test
    fun `the same list gets the same index, so typing does not rebuild it`() {
        val titles = listOf("Scream 4", "Backrooms")

        assertThat(cache.indexOf(titles)).isSameInstanceAs(cache.indexOf(titles))
    }

    @Test
    fun `a changed catalog gets a fresh index`() {
        val before = listOf("Scream 4")
        val after = listOf("Scream 4", "Backrooms")

        val old = cache.indexOf(before)
        val fresh = cache.indexOf(after)

        assertThat(fresh).isNotSameInstanceAs(old)
        assertThat(fresh.search("back")).isEqualTo(listOf("Backrooms"))
    }
}
