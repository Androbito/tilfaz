import com.android.build.api.dsl.LibraryExtension
import com.androbito.tilfaz.buildlogic.configureKotlinAndroid
import com.androbito.tilfaz.buildlogic.libs
import com.androbito.tilfaz.buildlogic.versionInt
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.library")
            apply(plugin = "org.jlleitschuh.gradle.ktlint")

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                testOptions.targetSdk = libs.versionInt("targetSdk")
                testOptions.unitTests.isIncludeAndroidResources = true
                defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                // Each library gets its own namespace derived from the module path: :core:data -> com.androbito.tilfaz.core.data
                namespace = "com.androbito.tilfaz" + path.replace(':', '.').replace('-', '_')
            }
        }
    }
}
