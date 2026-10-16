package com.androbito.tilfaz.feature.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androbito.tilfaz.core.domain.repository.LiveRepository
import com.androbito.tilfaz.core.domain.repository.PlaybackSettingsRepository
import com.androbito.tilfaz.core.domain.repository.SeriesRepository
import com.androbito.tilfaz.core.domain.repository.VodRepository
import com.androbito.tilfaz.core.domain.repository.WatchProgressRepository
import com.androbito.tilfaz.core.domain.usecase.BuildStreamSourceUseCase
import com.androbito.tilfaz.core.domain.usecase.NowNextResolver
import com.androbito.tilfaz.core.domain.usecase.ResumePositionPolicy
import com.androbito.tilfaz.core.model.ContentType
import com.androbito.tilfaz.core.model.LiveChannel
import com.androbito.tilfaz.core.model.NowNext
import com.androbito.tilfaz.core.model.PlaybackSettings
import com.androbito.tilfaz.core.model.Playable
import com.androbito.tilfaz.core.model.StreamFormat
import com.androbito.tilfaz.core.model.StreamSource
import com.androbito.tilfaz.core.model.WatchProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

enum class PlayerErrorKind { SOURCE, NETWORK, DECODER, NOT_FOUND, UNKNOWN }

data class PlayerError(val kind: PlayerErrorKind, val detail: String?)

data class PlayerUiState(
    val source: StreamSource? = null,
    val settings: PlaybackSettings = PlaybackSettings(),
    val startPositionMs: Long = 0L,
    val isLive: Boolean = false,
    val nowNext: NowNext = NowNext.Empty,
    val zapList: List<LiveChannel> = emptyList(),
    val error: PlayerError? = null,
    val isLoading: Boolean = true,
    val formatOverride: StreamFormat? = null,
) {
    val title: String get() = source?.playable?.title.orEmpty()
    val canSwitchFormat: Boolean get() = isLive
    val alternativeFormat: StreamFormat
        get() = if ((formatOverride ?: settings.liveFormat) == StreamFormat.HLS) StreamFormat.MPEG_TS else StreamFormat.HLS
}

sealed interface PlayerEvent {
    data class PlaybackFailed(val error: PlayerError) : PlayerEvent
    data object Retry : PlayerEvent
    data object SwitchFormat : PlayerEvent
    data object ZapNext : PlayerEvent
    data object ZapPrevious : PlayerEvent
    data class ProgressTick(val positionMs: Long, val durationMs: Long) : PlayerEvent
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val liveRepository: LiveRepository,
    private val vodRepository: VodRepository,
    private val seriesRepository: SeriesRepository,
    private val progressRepository: WatchProgressRepository,
    private val settingsRepository: PlaybackSettingsRepository,
    private val buildStreamSource: BuildStreamSourceUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val kind: String = checkNotNull(savedStateHandle[PlayerRoute.ARG_KIND])
    private var itemId: String = checkNotNull(savedStateHandle[PlayerRoute.ARG_ITEM_ID])
    private val parentId: Int = savedStateHandle[PlayerRoute.ARG_PARENT_ID] ?: 0
    private val categoryId: String? = savedStateHandle[PlayerRoute.ARG_CATEGORY_ID]
    private val requestedStart: Long = savedStateHandle[PlayerRoute.ARG_START_POSITION] ?: 0L

    private val _state = MutableStateFlow(PlayerUiState(isLive = kind == PlayerRoute.KIND_LIVE))
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var lastSavedPositionMs = -1L

    init {
        viewModelScope.launch { load() }
    }

    fun onEvent(event: PlayerEvent) {
        when (event) {
            is PlayerEvent.PlaybackFailed -> _state.update { it.copy(error = event.error, isLoading = false) }
            PlayerEvent.Retry -> viewModelScope.launch { load() }
            PlayerEvent.SwitchFormat -> viewModelScope.launch {
                val next = _state.value.alternativeFormat
                _state.update { it.copy(formatOverride = next) }
                // Make the choice stick: if HLS works where TS failed, the user should not have to re-pick every time.
                settingsRepository.update { it.copy(liveFormat = next) }
                load()
            }
            PlayerEvent.ZapNext -> zap(+1)
            PlayerEvent.ZapPrevious -> zap(-1)
            is PlayerEvent.ProgressTick -> saveProgress(event.positionMs, event.durationMs)
        }
    }

    private suspend fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        val settings = settingsRepository.observe().first()
        val playable = resolvePlayable()
        if (playable == null) {
            _state.update { it.copy(isLoading = false, error = PlayerError(PlayerErrorKind.NOT_FOUND, "item $kind/$itemId")) }
            return
        }
        val startPosition = if (requestedStart > 0) {
            requestedStart
        } else {
            ResumePositionPolicy.startPositionMs(progressRepository.progress(playable.key), settings.resumePlayback)
        }
        buildStreamSource(playable, _state.value.formatOverride)
            .onSuccess { source ->
                _state.update { it.copy(source = source, settings = settings, startPositionMs = startPosition, isLoading = false) }
                if (playable is Playable.Live) {
                    loadLiveContext(playable.channel)
                }
            }
            .onFailure { e -> _state.update { it.copy(isLoading = false, error = PlayerError(PlayerErrorKind.UNKNOWN, e.message)) } }
    }

    private suspend fun resolvePlayable(): Playable? = when (kind) {
        PlayerRoute.KIND_LIVE -> itemId.toIntOrNull()?.let { liveRepository.channel(it) }?.let(Playable::Live)
        PlayerRoute.KIND_MOVIE -> itemId.toIntOrNull()?.let { vodRepository.movie(it) }?.let(Playable::Film)
        PlayerRoute.KIND_EPISODE -> seriesRepository.details(parentId).getOrNull()?.let { details ->
            details.seasons.flatMap { it.episodes }.firstOrNull { it.id == itemId }?.let { Playable.SeriesEpisode(it, details.series.name) }
        }
        else -> null
    }

    private suspend fun loadLiveContext(channel: LiveChannel) {
        val zapList = liveRepository.observeItems(categoryId).first()
        _state.update { it.copy(zapList = zapList) }
        val nowNext = liveRepository.shortEpg(channel.id).map { NowNextResolver.resolve(it, clock.instant()) }.getOrDefault(NowNext.Empty)
        _state.update { it.copy(nowNext = nowNext) }
    }

    private fun zap(direction: Int) {
        val current = _state.value
        val list = current.zapList
        val channel = (current.source?.playable as? Playable.Live)?.channel ?: return
        if (list.isEmpty()) return
        val index = list.indexOfFirst { it.id == channel.id }.takeIf { it >= 0 } ?: return
        val next = list[(index + direction + list.size) % list.size]
        itemId = next.id.toString()
        _state.update { it.copy(nowNext = NowNext.Empty) }
        viewModelScope.launch { load() }
    }

    private fun saveProgress(positionMs: Long, durationMs: Long) {
        val source = _state.value.source ?: return
        if (source.isLive || durationMs <= 0 || positionMs < 0) return
        if (lastSavedPositionMs >= 0 && kotlin.math.abs(positionMs - lastSavedPositionMs) < SAVE_INTERVAL_MS) return
        lastSavedPositionMs = positionMs
        val playable = source.playable
        val (type, poster, reference) = when (playable) {
            is Playable.Film -> Triple(ContentType.VOD, playable.movie.posterUrl, playable.movie.id.toString())
            is Playable.SeriesEpisode -> Triple(ContentType.SERIES, playable.episode.thumbnailUrl, "${playable.episode.seriesId}:${playable.episode.id}")
            is Playable.Live -> return
        }
        viewModelScope.launch {
            progressRepository.save(
                WatchProgress(
                    playableKey = playable.key,
                    contentType = type,
                    title = playable.title,
                    posterUrl = poster,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    updatedAt = clock.instant(),
                    reference = reference,
                ),
            )
        }
    }

    private companion object {
        const val SAVE_INTERVAL_MS = 10_000L
    }
}
