package com.androbito.tilfaz.feature.search

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.androbito.tilfaz.core.testing.fake.FakeLiveRepository
import com.androbito.tilfaz.core.testing.fake.FakeSeriesRepository
import com.androbito.tilfaz.core.testing.fake.FakeVodRepository
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import com.androbito.tilfaz.core.testing.rule.MainDispatcherRule
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class SearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @get:Rule val mainDispatcher = MainDispatcherRule(dispatcher)

    private val live = FakeLiveRepository().apply { items.value = Fixtures.liveChannels }
    private val vod = FakeVodRepository().apply { items.value = listOf(Fixtures.movie) }
    private val series = FakeSeriesRepository().apply { items.value = listOf(Fixtures.series) }
    private val viewModel = SearchViewModel(live, vod, series)

    @Test
    fun `short queries do not search`() = runTest(dispatcher) {
        viewModel.state.test {
            viewModel.onQueryChanged("a")
            advanceTimeBy(500)
            runCurrent()
            val state = expectMostRecentItem()
            assertThat(state.isTooShort).isTrue()
            assertThat(state.channels).isEqualTo(emptyList())
        }
    }

    @Test
    fun `query is debounced and searches the three catalogs`() = runTest(dispatcher) {
        viewModel.state.test {
            viewModel.onQueryChanged("20")
            viewModel.onQueryChanged("2026")
            advanceTimeBy(500)
            runCurrent()
            val state = expectMostRecentItem()
            assertThat(state.movies).containsExactly(Fixtures.movie)
            assertThat(state.series).containsExactly(Fixtures.series)
            assertThat(state.channels).isEqualTo(emptyList())
        }
    }

    @Test
    fun `no match is reported as empty`() = runTest(dispatcher) {
        viewModel.state.test {
            viewModel.onQueryChanged("zzzz")
            advanceTimeBy(500)
            runCurrent()
            assertThat(expectMostRecentItem().isEmpty).isTrue()
        }
    }
}
