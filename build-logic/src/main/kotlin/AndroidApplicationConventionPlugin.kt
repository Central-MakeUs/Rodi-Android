import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.plugin.compose")
            }
            extensions.configure<ApplicationExtension> {
                compileSdk = 37
                defaultConfig {
                    minSdk = 30
                    targetSdk = 36
                }
                configureJavaKotlin(this)
                buildFeatures {
                    compose = true
                }
            }
        }
    }
}
