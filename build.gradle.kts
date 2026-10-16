// Root build file. Module configuration lives in build-logic convention plugins.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.kover)
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.roborazzi) apply false
}

dependencies {
    // Aggregated coverage report: ./gradlew koverHtmlReport
    kover(projects.core.domain)
    kover(projects.core.data)
    kover(projects.core.network)
    kover(projects.feature.onboarding)
    kover(projects.feature.home)
    kover(projects.feature.live)
    kover(projects.feature.vod)
    kover(projects.feature.series)
    kover(projects.feature.player)
    kover(projects.feature.settings)
    kover(projects.feature.search)
}
