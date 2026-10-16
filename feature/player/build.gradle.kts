plugins {
    alias(libs.plugins.tilfaz.android.feature)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(projects.core.network) // shared OkHttpClient for the media data source
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.ui.compose)
    implementation(libs.okhttp)
}
