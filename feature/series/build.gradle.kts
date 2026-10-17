plugins {
    alias(libs.plugins.tilfaz.android.feature)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.androidx.paging.compose)
}
