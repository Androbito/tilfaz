package com.androbito.tilfaz.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.androbito.tilfaz.feature.home.HomeNavigation
import com.androbito.tilfaz.feature.home.HomeRoute
import com.androbito.tilfaz.feature.home.homeScreen
import com.androbito.tilfaz.feature.library.ContinueWatchingNavigation
import com.androbito.tilfaz.feature.library.ContinueWatchingRoute
import com.androbito.tilfaz.feature.library.FavoritesNavigation
import com.androbito.tilfaz.feature.library.FavoritesRoute
import com.androbito.tilfaz.feature.library.continueWatchingScreen
import com.androbito.tilfaz.feature.library.favoritesScreen
import com.androbito.tilfaz.feature.live.LiveRoute
import com.androbito.tilfaz.feature.live.liveScreen
import com.androbito.tilfaz.feature.onboarding.SignInRoute
import com.androbito.tilfaz.feature.onboarding.signInScreen
import com.androbito.tilfaz.feature.player.PlayerRoute
import com.androbito.tilfaz.feature.player.playerScreen
import com.androbito.tilfaz.feature.series.SeriesDetailsRoute
import com.androbito.tilfaz.feature.series.SeriesRoute
import com.androbito.tilfaz.feature.series.seriesDetailsScreen
import com.androbito.tilfaz.feature.series.seriesScreen
import com.androbito.tilfaz.feature.settings.SettingsRoute
import com.androbito.tilfaz.feature.settings.settingsScreen
import com.androbito.tilfaz.feature.vod.MovieDetailsRoute
import com.androbito.tilfaz.feature.vod.MoviesRoute
import com.androbito.tilfaz.feature.vod.movieDetailsScreen
import com.androbito.tilfaz.feature.vod.moviesScreen

/**
 * The only place that knows about every feature. Features expose routes and callbacks; this maps the callbacks
 * to destinations, so no feature module depends on another.
 */
@Composable
fun TilfazNavHost(startSignedIn: Boolean) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = if (startSignedIn) HomeRoute else SignInRoute) {
        signInScreen(
            onSignedIn = {
                navController.navigate(HomeRoute) { popUpTo(SignInRoute) { inclusive = true } }
            }
        )

        homeScreen(
            HomeNavigation(
                onOpenLive = { navController.navigate(LiveRoute()) },
                onOpenMovies = { navController.navigate(MoviesRoute()) },
                onOpenSeries = { navController.navigate(SeriesRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onOpenFavorites = { navController.navigate(FavoritesRoute) },
                onOpenContinueWatching = { navController.navigate(ContinueWatchingRoute) }
            )
        )

        liveScreen(onPlayChannel = { channelId, categoryId -> navController.navigate(PlayerRoute.live(channelId, categoryId)) })

        moviesScreen(onOpenMovie = { navController.navigate(MovieDetailsRoute(it)) })
        movieDetailsScreen(
            onPlayMovie = { movieId, start -> navController.navigate(PlayerRoute.movie(movieId, start)) },
            onOpenMovie = { navController.navigate(MovieDetailsRoute(it)) }
        )

        seriesScreen(onOpenSeries = { navController.navigate(SeriesDetailsRoute(it)) })
        seriesDetailsScreen(
            onPlayEpisode = { seriesId, episodeId, start -> navController.navigate(PlayerRoute.episode(seriesId, episodeId, start)) },
            onOpenSeries = { navController.navigate(SeriesDetailsRoute(it)) }
        )

        favoritesScreen(
            FavoritesNavigation(
                onPlayChannel = { navController.navigate(PlayerRoute.live(it, categoryId = null)) },
                onOpenMovie = { navController.navigate(MovieDetailsRoute(it)) },
                onOpenSeries = { navController.navigate(SeriesDetailsRoute(it)) }
            )
        )

        continueWatchingScreen(
            ContinueWatchingNavigation(
                onPlayMovie = { movieId, start -> navController.navigate(PlayerRoute.movie(movieId, start)) },
                onPlayEpisode = { seriesId, episodeId, start -> navController.navigate(PlayerRoute.episode(seriesId, episodeId, start)) }
            )
        )

        playerScreen(onExit = { navController.popBackStack() })

        settingsScreen(
            onSignedOut = {
                navController.navigate(SignInRoute) { popUpTo(0) { inclusive = true } }
            }
        )
    }
}
