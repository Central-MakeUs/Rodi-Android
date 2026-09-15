import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class KoverConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlinx.kover")
            extensions.configure<KoverProjectExtension> {
                reports {
                    filters {
                        excludes {
                            // 커버리지 수치가 사람이 쓴 코드만 반영하도록 생성 코드를 뺀다.
                            classes(
                                "*.Hilt_*",
                                "*_Hilt*",
                                "*_Factory",
                                "*_Factory\$*",
                                "*Module_*Factory",
                                "*Module_*Factory\$*",
                                "*_Impl",
                                "*_Impl\$*",
                                "*_MembersInjector",
                                "*_GeneratedInjector",
                                "hilt_aggregated_deps.*",
                                "dagger.hilt.internal.*",
                                "*.BuildConfig",
                                "*.R",
                                "*.R\$*",
                                "*ComposableSingletons*",
                            )
                            annotatedBy(
                                "androidx.compose.ui.tooling.preview.Preview*",
                                "dagger.internal.DaggerGenerated",
                            )
                        }
                    }
                }
            }
        }
    }
}
