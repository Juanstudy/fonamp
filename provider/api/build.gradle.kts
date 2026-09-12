plugins {
    alias(libs.plugins.agp.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.fonamp.provider.api"
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
    // Android library with Kotlin-only sources: the single allowed Android import
    // in this module is Media3 MediaItem (design §2).
    implementation(libs.media3.common)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
