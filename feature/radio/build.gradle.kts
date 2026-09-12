plugins {
    alias(libs.plugins.agp.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.fonamp.feature.radio"
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
    // Radio UI: RadioBrowserSource + database/network/player/ui (design §1/§9).
    implementation(project(":provider:api"))
    implementation(project(":provider:radio"))
    implementation(project(":core:player"))
    implementation(project(":core:ui"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))

    implementation(libs.media3.common)
    implementation(libs.lifecycle.runtime.compose)
    // DirectoryCache signatures expose kotlinx.serialization types.
    implementation(libs.serialization.json)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation("androidx.compose.material:material-icons-core")
    // Radio/Public/Tag/Favorite live in the extended set; still generic M3
    // icons, no custom art (Slice F precedent).
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    releaseImplementation("androidx.compose.ui:ui-test-manifest")
}
