package com.androbito.tilfaz.core.data.mapper

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import assertk.assertions.prop
import com.androbito.tilfaz.core.model.DomainError
import com.androbito.tilfaz.core.model.DomainException
import com.androbito.tilfaz.core.model.StreamFormat
import com.androbito.tilfaz.core.network.dto.AuthResponseDto
import com.androbito.tilfaz.core.network.dto.ServerInfoDto
import com.androbito.tilfaz.core.network.dto.UserInfoDto
import com.androbito.tilfaz.core.testing.fixture.Fixtures
import org.junit.Test
import java.time.Instant

class AuthMapperTest {
    private fun user(auth: Boolean? = true, status: String? = "Active", exp: Long? = 1_804_000_000L) = AuthResponseDto(
        userInfo = UserInfoDto(auth = auth, status = status, expDateEpochSeconds = exp, maxConnections = 2, allowedOutputFormats = listOf("m3u8", "ts", "rtmp")),
        serverInfo = ServerInfoDto(timezone = "Europe/Paris"),
    )

    @Test
    fun `active account maps status, expiry, connections and known formats`() {
        val status = AuthMapper.toStatus(user(), Fixtures.now)

        assertThat(status.isActive).isTrue()
        assertThat(status.expiresAt).isEqualTo(Instant.ofEpochSecond(1_804_000_000L))
        assertThat(status.maxConnections).isEqualTo(2)
        assertThat(status.allowedOutputFormats).containsExactly(StreamFormat.HLS, StreamFormat.MPEG_TS)
        assertThat(status.serverTimeZone).isEqualTo("Europe/Paris")
    }

    @Test
    fun `auth zero is unauthorized even with http 200`() {
        assertFailure { AuthMapper.toStatus(user(auth = false), Fixtures.now) }
            .isInstanceOf<DomainException>().prop(DomainException::error).isEqualTo(DomainError.Unauthorized)
    }

    @Test
    fun `past expiry date or expired status is AccountExpired`() {
        assertFailure { AuthMapper.toStatus(user(exp = 1_000L), Fixtures.now) }
            .isInstanceOf<DomainException>().prop(DomainException::error).isEqualTo(DomainError.AccountExpired)
        assertFailure { AuthMapper.toStatus(user(status = "Expired", exp = null), Fixtures.now) }
            .isInstanceOf<DomainException>().prop(DomainException::error).isEqualTo(DomainError.AccountExpired)
    }

    @Test
    fun `disabled or banned status is unauthorized`() {
        assertFailure { AuthMapper.toStatus(user(status = "Banned"), Fixtures.now) }
            .isInstanceOf<DomainException>().prop(DomainException::error).isEqualTo(DomainError.Unauthorized)
    }

    @Test
    fun `missing user_info is a bad response`() {
        assertFailure { AuthMapper.toStatus(AuthResponseDto(), Fixtures.now) }
            .isInstanceOf<DomainException>().prop(DomainException::error).isInstanceOf<DomainError.BadResponse>()
    }
}
