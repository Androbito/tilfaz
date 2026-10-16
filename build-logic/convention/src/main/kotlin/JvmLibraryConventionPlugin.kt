import com.androbito.tilfaz.buildlogic.configureKotlinJvm
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

/** Pure Kotlin module: cannot touch the Android SDK by construction. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.jvm")
            apply(plugin = "org.jlleitschuh.gradle.ktlint")
            apply(plugin = "org.jetbrains.kotlinx.kover")
            configureKotlinJvm()
        }
    }
}
