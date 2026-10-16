package com.androbito.tilfaz.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/** Compose setup shared by the app and every UI module. */
internal fun Project.configureAndroidCompose(commonExtension: CommonExtension) {
    commonExtension.apply {
        buildFeatures.compose = true
    }

    dependencies {
        val bom = libs.findLibrary("androidx-compose-bom").get()
        add("implementation", platform(bom))
        add("implementation", libs.findLibrary("androidx-compose-foundation").get())
        add("implementation", libs.findLibrary("androidx-compose-ui").get())
        add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())
        add("implementation", libs.findLibrary("androidx-tv-material").get())
        add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
        add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
        add("androidTestImplementation", platform(bom))
    }

    extensions.findByType(ComposeCompilerGradlePluginExtension::class.java)?.apply {
        // Compose compiler metrics and reports: ./gradlew assembleRelease -PenableComposeCompilerReports=true
        if (project.providers.gradleProperty("enableComposeCompilerReports").isPresent) {
            val dir = project.layout.buildDirectory.dir("compose-reports")
            metricsDestination.set(dir)
            reportsDestination.set(dir)
        }
        stabilityConfigurationFiles.add(
            project.rootProject.layout.projectDirectory.file("compose-stability.conf"),
        )
    }
}
