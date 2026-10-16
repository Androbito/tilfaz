package com.androbito.tilfaz.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/** Shared Android configuration for application and library modules. */
internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension) {
    commonExtension.apply {
        compileSdk = libs.versionInt("compileSdk")

        defaultConfig.minSdk = libs.versionInt("minSdk")

        compileOptions.apply {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }

        lint.apply {
            abortOnError = true
            warningsAsErrors = false
            checkDependencies = false
        }
    }

    // AGP 9 built-in Kotlin registers the `kotlin` block itself (on the project, or nested in `android`).
    val kotlinExtension = extensions.findByName("kotlin") ?: commonExtension.extensions.findByName("kotlin")
    @Suppress("UNCHECKED_CAST")
    (kotlinExtension as? HasConfigurableKotlinCompilerOptions<KotlinJvmCompilerOptions>)?.compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(false)
        freeCompilerArgs.addAll(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
        )
    }

    dependencies {
        add("testImplementation", libs.findLibrary("junit").get())
        add("testImplementation", libs.findLibrary("assertk").get())
        add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
        add("testImplementation", libs.findLibrary("turbine").get())
    }
}

/** Shared configuration for pure Kotlin (JVM) modules such as :core:model and :core:domain. */
internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    extensions.configure<KotlinJvmProjectExtension> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.addAll("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
        }
    }

    dependencies {
        add("testImplementation", libs.findLibrary("junit").get())
        add("testImplementation", libs.findLibrary("assertk").get())
        add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
        add("testImplementation", libs.findLibrary("turbine").get())
    }
}
