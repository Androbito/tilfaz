package com.androbito.tilfaz.core.domain.usecase

import com.androbito.tilfaz.core.domain.repository.AccountRepository
import com.androbito.tilfaz.core.domain.repository.PlaybackSettingsRepository
import com.androbito.tilfaz.core.model.DomainError
import com.androbito.tilfaz.core.model.DomainException
import com.androbito.tilfaz.core.model.Playable
import com.androbito.tilfaz.core.model.StreamFormat
import com.androbito.tilfaz.core.model.StreamSource
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Combines the signed-in account and the playback settings into a URL the player can open. */
class BuildStreamSourceUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val settingsRepository: PlaybackSettingsRepository
) {
    suspend operator fun invoke(playable: Playable, formatOverride: StreamFormat? = null): Result<StreamSource> {
        val account = accountRepository.observeAccount().first()
            ?: return Result.failure(DomainException(DomainError.NotSignedIn))
        val settings = settingsRepository.observe().first().let { s ->
            if (formatOverride != null) s.copy(liveFormat = formatOverride) else s
        }
        return Result.success(StreamUrlBuilder.build(account, playable, settings))
    }
}
