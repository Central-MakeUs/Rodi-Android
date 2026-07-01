import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion

internal fun configureJavaKotlin(commonExtension: CommonExtension) {
    commonExtension.compileOptions.apply {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
