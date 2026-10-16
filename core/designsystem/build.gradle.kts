plugins {
    alias(libs.plugins.tilfaz.android.library.compose)
    alias(libs.plugins.roborazzi)
}

android {
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    api(libs.androidx.tv.material)
    api(libs.androidx.compose.material.icons.extended)
    api(libs.coil.compose)
    implementation(projects.core.model)

    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.ext.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

roborazzi {
    // Record: ./gradlew :core:designsystem:recordRoborazziDebug   Verify: ./gradlew :core:designsystem:verifyRoborazziDebug
    outputDir.set(file("src/test/screenshots"))
}
