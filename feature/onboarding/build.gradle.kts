plugins {
    alias(libs.plugins.tilfaz.android.feature)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.ext.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
