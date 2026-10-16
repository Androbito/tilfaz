package com.androbito.tilfaz.core.network.dto

import com.androbito.tilfaz.core.network.serialization.LenientBooleanSerializer
import com.androbito.tilfaz.core.network.serialization.LenientIntSerializer
import com.androbito.tilfaz.core.network.serialization.LenientLongSerializer
import com.androbito.tilfaz.core.network.serialization.LenientStringListSerializer
import com.androbito.tilfaz.core.network.serialization.LenientStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthResponseDto(
    @SerialName("user_info") val userInfo: UserInfoDto? = null,
    @SerialName("server_info") val serverInfo: ServerInfoDto? = null
)

@Serializable
data class UserInfoDto(
    @Serializable(LenientStringSerializer::class) val username: String? = null,
    @Serializable(LenientStringSerializer::class) val message: String? = null,
    @Serializable(LenientBooleanSerializer::class) val auth: Boolean? = null,
    @Serializable(LenientStringSerializer::class) val status: String? = null,
    @SerialName("exp_date") @Serializable(LenientLongSerializer::class) val expDateEpochSeconds: Long? = null,
    @SerialName("is_trial") @Serializable(LenientBooleanSerializer::class) val isTrial: Boolean? = null,
    @SerialName("active_cons") @Serializable(LenientIntSerializer::class) val activeConnections: Int? = null,
    @SerialName("created_at") @Serializable(LenientLongSerializer::class) val createdAtEpochSeconds: Long? = null,
    @SerialName("max_connections") @Serializable(LenientIntSerializer::class) val maxConnections: Int? = null,
    @SerialName("allowed_output_formats") @Serializable(LenientStringListSerializer::class) val allowedOutputFormats: List<String> = emptyList()
)

@Serializable
data class ServerInfoDto(
    @Serializable(LenientStringSerializer::class) val url: String? = null,
    @Serializable(LenientStringSerializer::class) val port: String? = null,
    @SerialName("https_port") @Serializable(LenientStringSerializer::class) val httpsPort: String? = null,
    @SerialName("server_protocol") @Serializable(LenientStringSerializer::class) val serverProtocol: String? = null,
    @Serializable(LenientStringSerializer::class) val timezone: String? = null,
    @SerialName("timestamp_now") @Serializable(LenientLongSerializer::class) val timestampNow: Long? = null
)
