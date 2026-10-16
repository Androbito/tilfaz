plugins {
    alias(libs.plugins.tilfaz.android.library)
    alias(libs.plugins.tilfaz.android.hilt)
    alias(libs.plugins.tilfaz.android.room)
}

dependencies {
    api(projects.core.model)
    implementation(libs.kotlinx.coroutines.core)

    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.ext.junit)
}
