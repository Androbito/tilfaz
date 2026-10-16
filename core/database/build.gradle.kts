plugins {
    alias(libs.plugins.tilfaz.android.library)
    alias(libs.plugins.tilfaz.android.hilt)
    alias(libs.plugins.tilfaz.android.room)
}

dependencies {
    api(projects.core.model)
    // TilfazDatabase extends RoomDatabase and is injected into :core:data, so the supertype must be visible there.
    api(libs.androidx.room.runtime)
    implementation(libs.kotlinx.coroutines.core)

    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.ext.junit)
}
