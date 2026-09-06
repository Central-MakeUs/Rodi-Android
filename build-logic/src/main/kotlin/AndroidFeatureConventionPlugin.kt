import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("dororong.rodi.android.library.compose")
                apply("dororong.rodi.android.hilt")
            }
            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
            dependencies {
                add("implementation", project(":core:ui"))
                add("implementation", libs.findLibrary("androidx-activity-compose").get())
                add("implementation", libs.findBundle("lifecycle-compose").get())
                add("implementation", libs.findBundle("hilt-compose").get())
                add("ksp", libs.findLibrary("hilt-compiler").get())
                add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
            }
        }
    }
}
