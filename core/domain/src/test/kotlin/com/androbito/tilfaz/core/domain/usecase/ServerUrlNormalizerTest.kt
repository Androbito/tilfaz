package com.androbito.tilfaz.core.domain.usecase

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.androbito.tilfaz.core.domain.usecase.ServerUrlNormalizer.Outcome
import com.androbito.tilfaz.core.domain.usecase.ServerUrlNormalizer.Reason
import org.junit.Test

class ServerUrlNormalizerTest {
    @Test
    fun `host and port without scheme gets http`() {
        assertThat(ServerUrlNormalizer.normalize("tv.example.com:8080")).isEqualTo(Outcome.Valid("http://tv.example.com:8080"))
    }

    @Test
    fun `trailing slash and path are dropped`() {
        assertThat(ServerUrlNormalizer.normalize("http://tv.example.com:8080/get.php?username=a&password=b"))
            .isEqualTo(Outcome.Valid("http://tv.example.com:8080"))
    }

    @Test
    fun `default ports are omitted`() {
        assertThat(ServerUrlNormalizer.normalize("https://tv.example.com:443/")).isEqualTo(Outcome.Valid("https://tv.example.com"))
        assertThat(ServerUrlNormalizer.normalize("HTTP://TV.Example.com:80")).isEqualTo(Outcome.Valid("http://tv.example.com"))
    }

    @Test
    fun `surrounding whitespace is ignored`() {
        assertThat(ServerUrlNormalizer.normalize("  http://1.2.3.4:25461  ")).isEqualTo(Outcome.Valid("http://1.2.3.4:25461"))
    }

    @Test
    fun `empty input is rejected`() {
        assertThat(ServerUrlNormalizer.normalize("   ")).isEqualTo(Outcome.Invalid(Reason.EMPTY))
    }

    @Test
    fun `non http schemes are rejected`() {
        assertThat(ServerUrlNormalizer.normalize("rtmp://tv.example.com")).isEqualTo(Outcome.Invalid(Reason.UNSUPPORTED_SCHEME))
    }

    @Test
    fun `garbage is rejected as unparseable`() {
        assertThat(ServerUrlNormalizer.normalize("http://exa mple.com")).isEqualTo(Outcome.Invalid(Reason.UNPARSEABLE))
    }
}
