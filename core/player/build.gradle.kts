plugins {
    alias(libs.plugins.agp.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.fonamp.core.player"
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
}

dependencies {
    // Single ExoPlayer lineage: identical media3 pin for every artifact (design §0).
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)
    implementation(libs.media3.datasource)
    implementation(libs.media3.common)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
}
