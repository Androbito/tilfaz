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

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Signing is configured from CI secrets; see docs/release.md.
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
    }

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
    implementation(projects.feature.search)

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
}
