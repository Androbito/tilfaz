package com.androbito.tilfaz.core.domain.usecase

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.DomainError
import com.androbito.tilfaz.core.model.DomainException
import com.androbito.tilfaz.core.testing.fake.FakeLiveRepository
import com.androbito.tilfaz.core.testing.fake.FakeSeriesRepository
import com.androbito.tilfaz.core.testing.fake.FakeVodRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SyncCatalogUseCaseTest {
    private val live = FakeLiveRepository()
    private val vod = FakeVodRepository()
    private val series = FakeSeriesRepository()
    private val sync = SyncCatalogUseCase(live, vod, series)

    @Test
    fun `refreshes every catalog by default`() = runTest {
        val report = sync()

        assertThat(report.isFullSuccess).isTrue()
        assertThat(live.refreshCalls + vod.refreshCalls + series.refreshCalls).isEqualTo(3)
    }

    @Test
    fun `one failure does not stop the others and is reported per type`() = runTest {
        vod.refreshResult = Result.failure(DomainException(DomainError.Network))

        val report = sync()

        assertThat(report.failures).isEqualTo(mapOf(ContentType.VOD to DomainError.Network))
        assertThat(live.refreshCalls).isEqualTo(1)
        assertThat(series.refreshCalls).isEqualTo(1)
    }

    @Test
    fun `can refresh a single catalog`() = runTest {
        sync(setOf(ContentType.LIVE))

        assertThat(live.refreshCalls).isEqualTo(1)
        assertThat(vod.refreshCalls).isEqualTo(0)
    }
}
