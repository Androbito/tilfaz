package com.androbito.tilfaz.feature.live

import androidx.lifecycle.SavedStateHandle
import androidx.paging.testing.asSnapshot
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.androbito.tilfaz.core.domain.usecase.ToggleFavoriteUseCase
import com.androbito.tilfaz.core.model.LiveChannel
import com.androbito.tilfaz.core.testing.fake.FakeFavoritesRepository
import com.androbito.tilfaz.core.testing.fake.FakeLiveRepository
import com.androbito.tilfaz.core.testing.fake.FakeRecentChannelsRepository
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import com.androbito.tilfaz.core.testing.rule.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LiveViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val live = FakeLiveRepository().apply {
        categories.value = Fixtures.liveCategories
        items.value = Fixtures.liveChannels
    }
    private val favorites = FakeFavoritesRepository()
    private val recent = FakeRecentChannelsRepository { id -> Fixtures.liveChannels.firstOrNull { it.id == id } }
    private val viewModel by lazy { LiveViewModel(SavedStateHandle(), live, favorites, recent, ToggleFavoriteUseCase(favorites), Fixtures.fixedClock) }

    /** What the list shows now. The sources never complete and asSnapshot waits for that, so only the first set of pages is taken. */
    private suspend fun shown(): List<LiveChannel> = viewModel.channels.take(1).asSnapshot()

    /** Typing pauses before the database is asked; tests let that pause pass. */
    private fun typed(text: String) {
        viewModel.onEvent(LiveEvent.QueryChanged(text))
        mainDispatcher.dispatcher.scheduler.advanceTimeBy(LiveViewModel.SEARCH_DELAY_MS + 1)
        mainDispatcher.dispatcher.scheduler.runCurrent()
    }

    @Test
    fun `virtual categories come first with live counts`() = runTest {
        viewModel.state.test {
            val categories = expectMostRecentItem().categories
            assertThat(categories.map { it.kind }.take(3)).containsExactly(LiveCategoryUi.Kind.ALL, LiveCategoryUi.Kind.FAVORITES, LiveCategoryUi.Kind.HISTORY)
            assertThat(categories.first().count).isEqualTo(2)
            assertThat(categories.first { it.id == "1" }.count).isEqualTo(1)
        }
    }

    @Test
    fun `all channels are listed by default`() = runTest {
        assertThat(shown()).containsExactly(Fixtures.alJazeera, Fixtures.beinSports1)
    }

    @Test
    fun `selecting a provider category lists its channels and resets the preview to the first one`() = runTest {
        viewModel.state.test {
            viewModel.onEvent(LiveEvent.FocusChannel(Fixtures.alJazeera))
            viewModel.onEvent(LiveEvent.SelectCategory("1"))
            assertThat(expectMostRecentItem().focusedChannel).isEqualTo(Fixtures.beinSports1)
        }
        assertThat(shown()).containsExactly(Fixtures.beinSports1)
    }

    @Test
    fun `a query searches every channel, whichever category is selected`() = runTest {
        viewModel.onEvent(LiveEvent.SelectCategory("1"))
        typed("jazeera")

        assertThat(shown()).containsExactly(Fixtures.alJazeera)
    }

    @Test
    fun `an unknown query lists nothing`() = runTest {
        typed("nothing")

        assertThat(shown()).isEqualTo(emptyList())
    }

    @Test
    fun `clearing the query goes back to the selected category`() = runTest {
        viewModel.onEvent(LiveEvent.SelectCategory("1"))
        typed("jazeera")
        typed("")

        assertThat(shown()).containsExactly(Fixtures.beinSports1)
    }

    @Test
    fun `the preview follows the first search result`() = runTest {
        viewModel.state.test {
            viewModel.onEvent(LiveEvent.FocusChannel(Fixtures.alJazeera))
            typed("bein")
            assertThat(expectMostRecentItem().focusedChannel).isEqualTo(Fixtures.beinSports1)
        }
    }

    @Test
    fun `suggestions follow the query and list each channel once`() = runTest {
        val duplicate = Fixtures.alJazeera.copy(id = 103, name = "|FR| AL JAZEERA HD")
        live.items.value = Fixtures.liveChannels + duplicate
        viewModel.state.test {
            typed("al j")
            val state = expectMostRecentItem()
            assertThat(state.suggestions).containsExactly(Fixtures.alJazeera)
        }
        assertThat(shown().size).isEqualTo(2)
    }

    @Test
    fun `without a query there are no suggestions`() = runTest {
        viewModel.state.test { assertThat(expectMostRecentItem().suggestions).isEqualTo(emptyList()) }
    }

    @Test
    fun `the typed text is shown as typed, before the search has run`() = runTest {
        viewModel.state.test {
            viewModel.onEvent(LiveEvent.QueryChanged("beI"))
            assertThat(expectMostRecentItem().query).isEqualTo("beI")
        }
    }

    @Test
    fun `favourites category shows only favourited channels`() = runTest {
        viewModel.state.test {
            viewModel.onEvent(LiveEvent.ToggleFavorite(Fixtures.beinSports1.id))
            viewModel.onEvent(LiveEvent.SelectCategory(LiveCategoryUi.FAVORITES_ID))
            assertThat(expectMostRecentItem().favoriteIds).containsOnly(Fixtures.beinSports1.id)
        }
        assertThat(shown()).containsExactly(Fixtures.beinSports1)
    }

    @Test
    fun `focusing a channel loads its EPG once and resolves now and next`() = runTest {
        live.epg[Fixtures.alJazeera.id] = Result.success(
            listOf(Fixtures.epg(101, "News Hour", -30, 60), Fixtures.epg(101, "The Debate", 30, 60))
        )
        viewModel.state.test {
            viewModel.onEvent(LiveEvent.FocusChannel(Fixtures.alJazeera))
            viewModel.onEvent(LiveEvent.FocusChannel(Fixtures.alJazeera))
            val nowNext = expectMostRecentItem().nowNext.getValue(Fixtures.alJazeera.id)
            assertThat(nowNext.now?.title).isEqualTo("News Hour")
            assertThat(nowNext.next?.title).isEqualTo("The Debate")
        }
        assertThat(live.epgRequests).containsExactly(Fixtures.alJazeera.id)
    }

    @Test
    fun `the channel shown by default gets its EPG without anyone focusing it`() = runTest {
        live.epg[Fixtures.alJazeera.id] = Result.success(listOf(Fixtures.epg(101, "News Hour", -30, 60)))
        viewModel.state.test {
            val state = expectMostRecentItem()
            assertThat(state.focusedChannel).isEqualTo(Fixtures.alJazeera)
            assertThat(live.epgRequests).containsExactly(Fixtures.alJazeera.id)
        }
    }

    @Test
    fun `playing a channel records it in history`() = runTest {
        viewModel.state.test {
            viewModel.onEvent(LiveEvent.Play(Fixtures.beinSports1.id))
            viewModel.onEvent(LiveEvent.SelectCategory(LiveCategoryUi.HISTORY_ID))
            expectMostRecentItem()
        }
        assertThat(shown()).containsExactly(Fixtures.beinSports1)
    }

    @Test
    fun `nothing is previewed when the list is empty`() = runTest {
        live.items.value = emptyList()
        viewModel.state.test { assertThat(expectMostRecentItem().focusedChannel).isNull() }
    }

    @Test
    fun `opening the screen does not wait for the typing pause`() = runTest {
        viewModel.state.test {
            expectMostRecentItem()
            assertThat(mainDispatcher.dispatcher.scheduler.currentTime).isEqualTo(0L)
        }
    }
}
