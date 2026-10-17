package com.androbito.tilfaz.feature.live

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.androbito.tilfaz.core.domain.usecase.ToggleFavoriteUseCase
import com.androbito.tilfaz.core.testing.fake.FakeFavoritesRepository
import com.androbito.tilfaz.core.testing.fake.FakeLiveRepository
import com.androbito.tilfaz.core.testing.fake.FakeRecentChannelsRepository
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import com.androbito.tilfaz.core.testing.rule.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class LiveViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val live = FakeLiveRepository().apply {
        categories.value = Fixtures.liveCategories
        items.value = Fixtures.liveChannels
    }
    private val favorites = FakeFavoritesRepository()
    private val recent = FakeRecentChannelsRepository { id -> Fixtures.liveChannels.firstOrNull { it.id == id } }
    private val viewModel by lazy { LiveViewModel(SavedStateHandle(), live, favorites, recent, ToggleFavoriteUseCase(favorites), Fixtures.fixedClock) }

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
    fun `selecting a provider category filters channels and resets focus`() = runTest {
        viewModel.state.test {
            viewModel.onEvent(LiveEvent.FocusChannel(Fixtures.alJazeera.id))
            viewModel.onEvent(LiveEvent.SelectCategory("1"))
            val state = expectMostRecentItem()
            assertThat(state.channels).containsExactly(Fixtures.beinSports1)
            assertThat(state.focusedChannelId).isEqualTo(Fixtures.beinSports1.id)
        }
    }

    @Test
    fun `preview moves to the first result when the focused channel is filtered out`() = runTest {
        viewModel.state.test {
            viewModel.onEvent(LiveEvent.FocusChannel(Fixtures.alJazeera.id))
            viewModel.onEvent(LiveEvent.QueryChanged("bein"))
            val state = expectMostRecentItem()
            assertThat(state.channels).containsExactly(Fixtures.beinSports1)
            assertThat(state.focusedChannel).isEqualTo(Fixtures.beinSports1)
        }
    }

    @Test
    fun `favourites category shows only favourited channels`() = runTest {
        viewModel.state.test {
            viewModel.onEvent(LiveEvent.ToggleFavorite(Fixtures.beinSports1.id))
            viewModel.onEvent(LiveEvent.SelectCategory(LiveCategoryUi.FAVORITES_ID))
            val state = expectMostRecentItem()
            assertThat(state.channels).containsExactly(Fixtures.beinSports1)
            assertThat(state.favoriteIds).containsOnly(Fixtures.beinSports1.id)
        }
    }

    @Test
    fun `focusing a channel loads its EPG once and resolves now and next`() = runTest {
        live.epg[Fixtures.alJazeera.id] = Result.success(
            listOf(Fixtures.epg(101, "News Hour", -30, 60), Fixtures.epg(101, "The Debate", 30, 60))
        )
        viewModel.state.test {
            viewModel.onEvent(LiveEvent.FocusChannel(Fixtures.alJazeera.id))
            viewModel.onEvent(LiveEvent.FocusChannel(Fixtures.alJazeera.id))
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
            assertThat(state.focusedChannelId).isEqualTo(Fixtures.alJazeera.id)
            assertThat(live.epgRequests).containsExactly(Fixtures.alJazeera.id)
        }
    }

    @Test
    fun `search filters within the selected category`() = runTest {
        viewModel.state.test {
            viewModel.onEvent(LiveEvent.QueryChanged("jazeera"))
            assertThat(expectMostRecentItem().channels).containsExactly(Fixtures.alJazeera)
            viewModel.onEvent(LiveEvent.QueryChanged("nothing"))
            assertThat(expectMostRecentItem().channels).isEqualTo(emptyList())
        }
    }

    @Test
    fun `playing a channel records it in history`() = runTest {
        viewModel.state.test {
            viewModel.onEvent(LiveEvent.Play(Fixtures.beinSports1.id))
            viewModel.onEvent(LiveEvent.SelectCategory(LiveCategoryUi.HISTORY_ID))
            assertThat(expectMostRecentItem().channels).containsExactly(Fixtures.beinSports1)
        }
    }

    @Test
    fun `focused channel is null when the list is empty`() = runTest {
        live.items.value = emptyList()
        viewModel.state.test {
            assertThat(expectMostRecentItem().focusedChannelId).isNull()
        }
    }
}
