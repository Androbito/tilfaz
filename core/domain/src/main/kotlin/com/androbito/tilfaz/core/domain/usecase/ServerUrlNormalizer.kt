package com.androbito.tilfaz.core.domain.usecase

import java.net.URI
import java.net.URISyntaxException

/**
 * Users paste Xtream server addresses in every imaginable shape:
 * "example.com:8080", "http://example.com:8080/", "http://example.com/get.php?username=...".
 * This reduces them to "scheme://host[:port]" so every API and stream URL is built from the same root.
 */
object ServerUrlNormalizer {

    sealed interface Outcome {
        data class Valid(val url: String) : Outcome
        data class Invalid(val reason: Reason) : Outcome
    }

    enum class Reason { EMPTY, UNPARSEABLE, MISSING_HOST, UNSUPPORTED_SCHEME }

    fun normalize(raw: String): Outcome {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return Outcome.Invalid(Reason.EMPTY)

        val withScheme = if (SCHEME_REGEX.containsMatchIn(trimmed)) trimmed else "http://$trimmed"
        val uri = try {
            URI(withScheme)
        } catch (e: URISyntaxException) {
            return Outcome.Invalid(Reason.UNPARSEABLE)
        }

        val scheme = uri.scheme?.lowercase() ?: return Outcome.Invalid(Reason.UNPARSEABLE)
        if (scheme != "http" && scheme != "https") return Outcome.Invalid(Reason.UNSUPPORTED_SCHEME)
        val host = uri.host ?: return Outcome.Invalid(Reason.MISSING_HOST)

        val port = uri.port.takeIf { it > 0 && it != defaultPort(scheme) }
        return Outcome.Valid(buildString {
            append(scheme).append("://").append(host.lowercase())
            if (port != null) append(':').append(port)
        })
    }

    private fun defaultPort(scheme: String) = if (scheme == "https") 443 else 80

    private val SCHEME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")
}
