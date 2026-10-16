import com.android.build.api.dsl.ApplicationExtension
import com.androbito.tilfaz.buildlogic.configureAndroidCompose
import com.androbito.tilfaz.buildlogic.configureKotlinAndroid
import com.androbito.tilfaz.buildlogic.libs
import com.androbito.tilfaz.buildlogic.versionInt
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.application")
            apply(plugin = "org.jetbrains.kotlin.android")
            apply(plugin = "org.jetbrains.kotlin.plugin.compose")
            apply(plugin = "org.jlleitschuh.gradle.ktlint")

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                configureAndroidCompose(this)
                defaultConfig.targetSdk = libs.versionInt("targetSdk")
                testOptions.animationsDisabled = true
            }
        }
    }
}
