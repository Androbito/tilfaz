package com.androbito.tilfaz

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.request.SuccessResult
import com.androbito.tilfaz.core.designsystem.theme.TilfazTheme
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.EpgEntry
import com.androbito.tilfaz.core.model.Episode
import com.androbito.tilfaz.core.model.LiveChannel
import com.androbito.tilfaz.core.model.Movie
import com.androbito.tilfaz.core.model.MovieDetails
import com.androbito.tilfaz.core.model.NowNext
import com.androbito.tilfaz.core.model.Rating
import com.androbito.tilfaz.core.model.Season
import com.androbito.tilfaz.core.model.Series
import com.androbito.tilfaz.core.model.SeriesDetails
import com.androbito.tilfaz.core.model.SyncStatus
import com.androbito.tilfaz.feature.home.HomeNavigation
import com.androbito.tilfaz.feature.home.HomeScreen
import com.androbito.tilfaz.feature.home.HomeUiState
import com.androbito.tilfaz.feature.live.LiveCategoryUi
import com.androbito.tilfaz.feature.live.LiveScreen
import com.androbito.tilfaz.feature.live.LiveUiState
import com.androbito.tilfaz.feature.series.SeriesDetailsScreen
import com.androbito.tilfaz.feature.series.SeriesDetailsUiState
import com.androbito.tilfaz.feature.vod.MovieDetailsScreen
import com.androbito.tilfaz.feature.vod.MovieDetailsUiState
import com.github.takahirom.roborazzi.captureRoboImage
import java.time.Instant
import kotlinx.coroutines.flow.flowOf
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The Play Store screenshots, drawn from the real screens with invented data: no provider, no real channel, logo or artwork.
 * Run with `STORE_SCREENSHOTS=1 ./gradlew :app:testDebugUnitTest --tests '*StoreScreenshots*'`; the images land in store/screenshots.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-television-xhdpi")
class StoreScreenshots {

    @get:Rule
    val compose = createComposeRule()

    private val now = Instant.parse("2026-10-18T20:00:00Z")

    /** Every image address is answered with a gradient picked from the address, so posters and logos are abstract art. */
    @Before
    fun fakeArtwork() {
        // Only when asked: the images are committed, so an ordinary test run must not rewrite them.
        assumeTrue(System.getenv("STORE_SCREENSHOTS") == "1")
        // The Gradle plugin that sets this is not applied to :app.
        System.setProperty("roborazzi.test.record", "true")
        val palettes = listOf(
            Color.parseColor("#7F5AF0") to Color.parseColor("#2CB1BC"),
            Color.parseColor("#EF4565") to Color.parseColor("#F7B801"),
            Color.parseColor("#3DA9FC") to Color.parseColor("#6246EA"),
            Color.parseColor("#2CB67D") to Color.parseColor("#094067"),
            Color.parseColor("#FF8906") to Color.parseColor("#E53170")
        )
        val art = object : Interceptor {
            override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
                val (from, to) = palettes[Math.floorMod(chain.request.data.hashCode(), palettes.size)]
                val bitmap = Bitmap.createBitmap(300, 450, Bitmap.Config.ARGB_8888)
                val gradient = LinearGradient(0f, 0f, 300f, 450f, from, to, Shader.TileMode.CLAMP)
                Canvas(bitmap).drawRect(0f, 0f, 300f, 450f, Paint().apply { shader = gradient })
                return SuccessResult(image = bitmap.asImage(), request = chain.request, dataSource = DataSource.MEMORY)
            }
        }
        val loader = ImageLoader.Builder(ApplicationProvider.getApplicationContext()).components { add(art) }.build()
        SingletonImageLoader.setUnsafe(loader)
    }

    private val channels = listOf("Demo News", "Sample Sports", "Nature Channel", "Kids Corner", "Music Live", "Cinema One", "Weather Now", "Cooking Time")
        .mapIndexed { i, name -> LiveChannel(100 + i, i + 1, name, "logo-$i", "1", null) }

    private fun entry(channel: LiveChannel, title: String, startMin: Long, endMin: Long, description: String? = null) =
        EpgEntry(channel.id, title, description, now.plusSeconds(startMin * 60), now.plusSeconds(endMin * 60))

    private fun shoot(name: String, content: @Composable () -> Unit) {
        compose.setContent { TilfazTheme { content() } }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("../store/screenshots/$name.png")
    }

    @Test
    fun home() = shoot("1-home") {
        val synced = Instant.parse("2026-10-18T19:15:00Z")
        HomeScreen(
            state = HomeUiState(
                accountName = "Demo account",
                accountStatus = null,
                syncStatuses = ContentType.entries.associateWith { SyncStatus(it, synced, 1200) }
            ),
            onEvent = {},
            navigation = HomeNavigation({}, {}, {}, {}, {}, {}, {}, {})
        )
    }

    @Test
    fun live() = shoot("2-live") {
        val nowNext = channels.associate {
            it.id to NowNext(entry(it, "Evening programme", -30, 30, "A sample programme used for this screenshot."), entry(it, "Late show", 30, 90))
        }
        LiveScreen(
            state = LiveUiState(
                categories = listOf(
                    LiveCategoryUi(LiveCategoryUi.ALL_ID, "", channels.size, LiveCategoryUi.Kind.ALL),
                    LiveCategoryUi("1", "News", 3, LiveCategoryUi.Kind.PROVIDER),
                    LiveCategoryUi("2", "Sports", 3, LiveCategoryUi.Kind.PROVIDER),
                    LiveCategoryUi("3", "Kids", 2, LiveCategoryUi.Kind.PROVIDER)
                ),
                nowNext = nowNext,
                focusedChannel = channels[1],
                isLoading = false
            ),
            channels = flowOf(PagingData.from(channels)).collectAsLazyPagingItems(),
            onEvent = {},
            onPlay = {}
        )
    }

    private val movie = Movie(7, "Sample Movie", "poster-movie", "1", Rating(7.8), 2025, now, "mkv")

    @Test
    fun movie() = shoot("3-movie") {
        val details = MovieDetails(
            movie = movie,
            plot = "A short invented plot for the store screenshot. Nothing here is a real film.",
            cast = "Jane Doe, John Roe",
            director = "Alex Sample",
            genre = "Drama",
            releaseDate = "2025-05-01",
            durationSeconds = 6300,
            backdropUrls = emptyList(),
            trailerUrl = null
        )
        MovieDetailsScreen(state = MovieDetailsUiState(movie = movie, details = details), onEvent = {}, onPlay = {}, onOpenMovie = {})
    }

    @Test
    fun series() = shoot("4-series") {
        val series = Series(9, "Sample Series", "poster-series", "1", Rating(8.2), 2024, now, "An invented story for the store screenshot.", "Mystery")
        val episodes = (1..3).map { Episode("90$it", 9, 1, it, "Episode $it", "mkv", 2700, "Invented episode $it.", "thumb-$it", null) }
        val details = SeriesDetails(
            series = series,
            plot = series.plot,
            cast = "Jane Doe",
            director = "Alex Sample",
            genre = "Mystery",
            releaseDate = "2024-09-01",
            backdropUrls = emptyList(),
            seasons = listOf(Season(1, "Season 1", episodes))
        )
        SeriesDetailsScreen(
            state = SeriesDetailsUiState(series = series, details = details, isLoading = false, selectedSeason = 1),
            onEvent = {},
            onPlay = {},
            onOpenSeries = {}
        )
    }
}
