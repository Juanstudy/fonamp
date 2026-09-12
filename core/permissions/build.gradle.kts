plugins {
    alias(libs.plugins.agp.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.fonamp.core.permissions"
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
    testImplementation(libs.junit)
}
