import java.io.StringReader
import java.util.Properties

plugins {
    alias(libs.plugins.tilfaz.android.application)
    alias(libs.plugins.tilfaz.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// Optional, gitignored file with a test account to prefill the sign-in form (see dev-credentials.properties.example).
// Read through providers so editing it invalidates the configuration cache.
val devCredentials = Properties().apply {
    providers.fileContents(rootProject.layout.projectDirectory.file("dev-credentials.properties")).asText.orNull
        ?.let { load(StringReader(it)) }
}

val devCredentialKeys = setOf("SERVER_URL", "USERNAME", "PASSWORD")
if (devCredentials.isNotEmpty()) {
    val unknown = devCredentials.stringPropertyNames() - devCredentialKeys
    val missing = devCredentialKeys - devCredentials.stringPropertyNames()
    if (unknown.isNotEmpty() || missing.isNotEmpty()) {
        logger.warn("dev-credentials.properties: unknown keys $unknown, missing keys $missing (expected $devCredentialKeys)")
    }
}

fun devCredentialField(key: String): String {
    val value = devCredentials.getProperty(key, "").replace("\\", "\\\\").replace("\"", "\\\"")
    return "\"$value\""
}

android {
    namespace = "com.androbito.tilfaz"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.androbito.tilfaz"
        versionCode = (project.findProperty("versionCode") as String?)?.toInt() ?: 1
        versionName = (project.findProperty("versionName") as String?) ?: "0.1.0"
    }

    // The upload key for Play App Signing. It comes only from the environment or from Gradle properties (see docs/release.md and
    // ADR 0010), never from a file or a default in this repository. With none of the four values set, a release build is signed with
    // the debug key as before; with only some of them set, the build fails rather than ship something signed with the wrong key.
    val signingKeys = listOf("KEYSTORE_PATH", "KEYSTORE_PASSWORD", "KEY_ALIAS", "KEY_PASSWORD")
    val signingValues: Map<String, String> = signingKeys.mapNotNull { key ->
        providers.environmentVariable(key).orElse(providers.gradleProperty(key)).orNull?.takeIf { it.isNotBlank() }?.let { key to it }
    }.toMap()
    val missingSigningKeys = signingKeys - signingValues.keys
    if (missingSigningKeys.size == signingKeys.size) {
        logger.lifecycle("Release signing: no upload key configured, a release build is signed with the debug key.")
    } else if (missingSigningKeys.isNotEmpty()) {
        throw GradleException("Release signing is half configured, missing: $missingSigningKeys")
    } else {
        signingConfigs.create("release") {
            storeFile = file(signingValues.getValue("KEYSTORE_PATH"))
            storePassword = signingValues.getValue("KEYSTORE_PASSWORD")
            keyAlias = signingValues.getValue("KEY_ALIAS")
            keyPassword = signingValues.getValue("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // The upload key when configured (CI), the debug key otherwise; see the signing block above.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            // Never ship a test account.
            buildConfigField("String", "DEV_SERVER_URL", "\"\"")
            buildConfigField("String", "DEV_USERNAME", "\"\"")
            buildConfigField("String", "DEV_PASSWORD", "\"\"")
        }
        debug {
            applicationIdSuffix = ".debug"
            buildConfigField("String", "DEV_SERVER_URL", devCredentialField("SERVER_URL"))
            buildConfigField("String", "DEV_USERNAME", devCredentialField("USERNAME"))
            buildConfigField("String", "DEV_PASSWORD", devCredentialField("PASSWORD"))
        }
        // The debug build is far slower than a shipped one (debuggable, no code shrinking); this one is built like a release
        // but keeps the debug application id, signature and test account, so it installs over the debug build and measures real speed.
        create("staging") {
            initWith(getByName("debug"))
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            matchingFallbacks += listOf("release", "debug")
        }
    }

    testOptions.unitTests.isIncludeAndroidResources = true

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "META-INF/LICENSE*")
    }
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.domain)
    implementation(projects.core.data)
    implementation(projects.core.network)
    implementation(projects.core.database)
    implementation(projects.core.designsystem)

    implementation(projects.feature.onboarding)
    implementation(projects.feature.home)
    implementation(projects.feature.live)
    implementation(projects.feature.vod)
    implementation(projects.feature.series)
    implementation(projects.feature.player)
    implementation(projects.feature.settings)
    implementation(projects.feature.library)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.leanback)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.okhttp)

    testImplementation(projects.core.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.paging.compose)
    testImplementation(libs.androidx.paging.common)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.ext.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
