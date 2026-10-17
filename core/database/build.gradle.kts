plugins {
    alias(libs.plugins.tilfaz.android.library)
    alias(libs.plugins.tilfaz.android.hilt)
    alias(libs.plugins.tilfaz.android.room)
}

dependencies {
    api(projects.core.model)
    // TilfazDatabase extends RoomDatabase and is injected into :core:data, so the supertype must be visible there.
    api(libs.androidx.room.runtime)
    // The DAOs return PagingSource, so callers must see the type.
    api(libs.androidx.room.paging)
    implementation(libs.kotlinx.coroutines.core)

    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.ext.junit)
}
