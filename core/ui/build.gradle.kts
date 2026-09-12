plugins {
    alias(libs.plugins.agp.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.fonamp.core.ui"
    compileSdk = 35

    defaultConfig {
        minSdk = 29
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    // Generic M3 icons only (no custom iconography in v1, docs/03-design.md §4).
    implementation("androidx.compose.material:material-icons-core")
    // Pause/Stop live in the extended set; still generic M3 icons, no custom art.
    implementation("androidx.compose.material:material-icons-extended")

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    // Compose behavior-test harness (BOM-managed versions; direct coordinates
    // keep the single version source in gradle/libs.versions.toml untouched).
    testImplementation("androidx.compose.ui:ui-test-junit4")
    // Manifest-only stub declaring ComponentActivity so Robolectric resolves it.
    // Needed in both variants: unit tests run for debug AND release.
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    releaseImplementation("androidx.compose.ui:ui-test-manifest")
}
