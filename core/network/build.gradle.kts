plugins {
    alias(libs.plugins.tilfaz.android.library)
    alias(libs.plugins.tilfaz.android.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
}

android {
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(projects.core.model)

    api(libs.retrofit)
    api(libs.okhttp)
    api(libs.kotlinx.serialization.json)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
}
