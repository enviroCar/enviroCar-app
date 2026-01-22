import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val enableMapbox = gradleLocalProperties(rootDir, providers).getOrDefault("org.envirocar.map.enableMapbox", "true") == "true"
val enableMapLibre = gradleLocalProperties(rootDir, providers).getOrDefault("org.envirocar.map.enableMapLibre", "true") == "true"

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "org.envirocar.map"
    compileSdk = 35

    defaultConfig {
        minSdk = 23

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {

    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Provider: Mapbox
    if (enableMapbox) {
        implementation(libs.mapbox.maps.android)
    }

    // Provider: MapLibre
    if (enableMapLibre) {
        implementation(libs.maplibre.gl.android.sdk)
        implementation(libs.maplibre.gl.android.plugin.annotation.v9)
    }
}

// https://stackoverflow.com/a/69141612/12825435
tasks.withType<KotlinCompile>().configureEach {
    if (!enableMapbox) {
        exclude("**/provider/mapbox/**.kt")
    }
    if (!enableMapLibre) {
        exclude("**/provider/maplibre/**.kt")
    }
}
