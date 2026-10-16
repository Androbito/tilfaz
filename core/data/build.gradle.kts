plugins {
    alias(libs.plugins.tilfaz.android.library)
    alias(libs.plugins.tilfaz.android.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
}

dependencies {
    api(projects.core.model)
    api(projects.core.domain)
    implementation(projects.core.network)
    implementation(projects.core.database)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(projects.core.testing)
}
