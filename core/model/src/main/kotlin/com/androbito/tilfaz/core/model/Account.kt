package com.androbito.tilfaz.core.model

import java.time.Instant

/** Credentials and server of an Xtream Codes account. The server URL is normalised (scheme + host + optional port, no path). */
data class Account(
    val serverUrl: String,
    val username: String,
    val password: String,
) {
    val displayName: String get() = username
}

data class AccountStatus(
    val isActive: Boolean,
    val expiresAt: Instant?,
    val isTrial: Boolean,
    val maxConnections: Int,
    val activeConnections: Int,
    val allowedOutputFormats: List<StreamFormat>,
    val serverTimeZone: String?,
    val checkedAt: Instant,
)
