package com.androbito.tilfaz.feature.onboarding

/** Initial values of the sign-in form. The app module supplies them; only debug builds ever pass non-empty ones. */
data class SignInPrefill(val serverUrl: String, val username: String, val password: String) {
    companion object {
        val None = SignInPrefill(serverUrl = "", username = "", password = "")
    }
}
