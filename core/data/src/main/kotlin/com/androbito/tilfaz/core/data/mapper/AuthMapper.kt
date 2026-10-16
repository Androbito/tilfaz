package com.androbito.tilfaz.core.data.mapper

import com.androbito.tilfaz.core.model.AccountStatus
import com.androbito.tilfaz.core.model.DomainError
import com.androbito.tilfaz.core.model.DomainException
import com.androbito.tilfaz.core.model.StreamFormat
import com.androbito.tilfaz.core.network.dto.AuthResponseDto
import java.time.Instant

/**
 * Panels answer HTTP 200 for everything: bad credentials come back as auth=0, dead accounts as
 * status "Expired"/"Disabled"/"Banned". This turns those into the domain errors the UI understands.
 */
object AuthMapper {
    fun toStatus(response: AuthResponseDto, now: Instant): AccountStatus {
        val user = response.userInfo ?: throw DomainException(DomainError.BadResponse("missing user_info"))
        if (user.auth == false) throw DomainException(DomainError.Unauthorized)

        val status = user.status?.lowercase()
        val expiresAt = user.expDateEpochSeconds?.let(Instant::ofEpochSecond)
        val expired = (expiresAt != null && expiresAt.isBefore(now)) || status == "expired"
        if (expired) throw DomainException(DomainError.AccountExpired)
        if (status != null && status != "active") throw DomainException(DomainError.Unauthorized)

        return AccountStatus(
            isActive = true,
            expiresAt = expiresAt,
            isTrial = user.isTrial ?: false,
            maxConnections = user.maxConnections ?: 1,
            activeConnections = user.activeConnections ?: 0,
            allowedOutputFormats = user.allowedOutputFormats.mapNotNull(StreamFormat::fromExtension),
            serverTimeZone = response.serverInfo?.timezone,
            checkedAt = now,
        )
    }
}
