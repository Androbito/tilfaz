package com.androbito.tilfaz.core.domain.usecase

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.SyncStatus
import java.time.Duration
import java.time.Instant
import org.junit.Test

class CatalogRefreshPolicyTest {
    private val now = Instant.parse("2026-10-17T12:00:00Z")

    private fun status(type: ContentType, ageHours: Long?, items: Int = 100, syncing: Boolean = false) =
        SyncStatus(type, ageHours?.let { now.minus(Duration.ofHours(it)) }, items, syncing)

    @Test
    fun `a catalog never downloaded is due`() {
        val due = CatalogRefreshPolicy.due(listOf(status(ContentType.LIVE, ageHours = null, items = 0)), now)

        assertThat(due).containsOnly(ContentType.LIVE)
    }

    @Test
    fun `a catalog saved less than a day ago is left alone`() {
        val due = CatalogRefreshPolicy.due(listOf(status(ContentType.VOD, ageHours = 23)), now)

        assertThat(due).isEmpty()
    }

    @Test
    fun `a catalog saved a day ago or more is due`() {
        val due = CatalogRefreshPolicy.due(listOf(status(ContentType.VOD, ageHours = 24), status(ContentType.SERIES, ageHours = 80)), now)

        assertThat(due).containsOnly(ContentType.VOD, ContentType.SERIES)
    }

    @Test
    fun `only the stale ones are returned`() {
        val due = CatalogRefreshPolicy.due(
            listOf(status(ContentType.LIVE, ageHours = 2), status(ContentType.VOD, ageHours = 30), status(ContentType.SERIES, ageHours = 1)),
            now
        )

        assertThat(due).containsOnly(ContentType.VOD)
    }

    @Test
    fun `a catalog that is being downloaded right now is not started twice`() {
        val due = CatalogRefreshPolicy.due(listOf(status(ContentType.LIVE, ageHours = 50, syncing = true)), now)

        assertThat(due).isEmpty()
    }

    @Test
    fun `a provider with an empty catalog is not asked again at every launch`() {
        val due = CatalogRefreshPolicy.due(listOf(status(ContentType.SERIES, ageHours = 1, items = 0)), now)

        assertThat(due).isEmpty()
    }

    @Test
    fun `a clock that went backwards does not trigger a refresh`() {
        val due = CatalogRefreshPolicy.due(listOf(SyncStatus(ContentType.LIVE, now.plus(Duration.ofHours(5)), 10)), now)

        assertThat(due).isEmpty()
    }
}
