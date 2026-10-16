package com.androbito.tilfaz.feature.home

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.androbito.tilfaz.core.domain.usecase.SyncCatalogUseCase
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.DomainError
import com.androbito.tilfaz.core.model.DomainException
import com.androbito.tilfaz.core.model.SyncStatus
import com.androbito.tilfaz.core.testing.fake.FakeAccountRepository
import com.androbito.tilfaz.core.testing.fake.FakeLiveRepository
import com.androbito.tilfaz.core.testing.fake.FakeSeriesRepository
import com.androbito.tilfaz.core.testing.fake.FakeVodRepository
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import com.androbito.tilfaz.core.testing.rule.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val account = FakeAccountRepository(Fixtures.account).apply { status.value = Fixtures.activeStatus }
    private val live = FakeLiveRepository()
    private val vod = FakeVodRepository()
    private val series = FakeSeriesRepository()

    private fun viewModel() = HomeViewModel(account, live, vod, series, SyncCatalogUseCase(live, vod, series))

    @Test
    fun `empty cache triggers a full sync on start`() = runTest {
        viewModel()

        assertThat(live.refreshCalls).isEqualTo(1)
        assertThat(vod.refreshCalls).isEqualTo(1)
        assertThat(series.refreshCalls).isEqualTo(1)
    }

    @Test
    fun `cached catalogs are not re-fetched on start`() = runTest {
        live.syncStatus.value = SyncStatus(ContentType.LIVE, Fixtures.now, itemCount = 500)
        vod.syncStatus.value = SyncStatus(ContentType.VOD, Fixtures.now, itemCount = 9000)
        series.syncStatus.value = SyncStatus(ContentType.SERIES, Fixtures.now, itemCount = 300)

        viewModel()

        assertThat(live.refreshCalls + vod.refreshCalls + series.refreshCalls).isEqualTo(0)
    }

    @Test
    fun `state exposes account name, expiry and per type sync status`() = runTest {
        live.items.value = Fixtures.liveChannels
        val vm = viewModel()

        vm.state.test {
            val state = expectMostRecentItem()
            assertThat(state.accountName).isEqualTo("ayoub")
            assertThat(state.accountStatus?.expiresAt).isEqualTo(Fixtures.activeStatus.expiresAt)
            assertThat(state.status(ContentType.LIVE).itemCount).isEqualTo(2)
        }
    }

    @Test
    fun `a failed refresh is reported for that type only and can be dismissed`() = runTest {
        vod.refreshResult = Result.failure(DomainException(DomainError.Network))
        val vm = viewModel()

        vm.state.test {
            assertThat(expectMostRecentItem().syncErrors).isEqualTo(mapOf(ContentType.VOD to DomainError.Network))
            vm.onEvent(HomeEvent.ErrorShown(ContentType.VOD))
            assertThat(expectMostRecentItem().syncErrors.isEmpty()).isTrue()
        }
    }

    @Test
    fun `refresh of a single type only hits that repository`() = runTest {
        live.syncStatus.value = SyncStatus(ContentType.LIVE, Fixtures.now, 1)
        vod.syncStatus.value = SyncStatus(ContentType.VOD, Fixtures.now, 1)
        series.syncStatus.value = SyncStatus(ContentType.SERIES, Fixtures.now, 1)
        val vm = viewModel()

        vm.onEvent(HomeEvent.Refresh(ContentType.SERIES))

        assertThat(series.refreshCalls).isEqualTo(1)
        assertThat(live.refreshCalls).isEqualTo(0)
    }
}
