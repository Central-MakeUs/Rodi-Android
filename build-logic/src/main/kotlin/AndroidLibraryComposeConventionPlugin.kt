import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.plugin.compose")
            }
            extensions.configure<LibraryExtension> {
                compileSdk = 37
                defaultConfig.minSdk = 30
                configureJavaKotlin(this)
                buildFeatures {
                    compose = true
                }
            }
            extensions.configure<ComposeCompilerGradlePluginExtension> {
                // :core:domain 등 Compose 플러그인이 없는 모듈의 모델(Course, RouteResult 등)이
                // List/Map 필드 때문에 unstable로 분류되는 것을 이 설정 파일로 안정 처리한다.
                stabilityConfigurationFiles.add(
                    rootProject.layout.projectDirectory.file("compose_compiler_config.conf"),
                )
            }
            tasks.withType<Test>().configureEach {
                useJUnitPlatform()
            }
        }
    }
}
