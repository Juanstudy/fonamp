plugins {
    alias(libs.plugins.agp.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.fonamp.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.fonamp.app"
        minSdk = 29
        targetSdk = 35
        versionCode = 3
        versionName = "0.0.3"
    }

    buildTypes {
        // v1 ships a direct debug APK: no R8/minify (scaffold Req 6).
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
}

dependencies {
    // Wiring only (design §1/§9): features + core + providers. No feature→feature edges.
    implementation(project(":feature:library"))
    implementation(project(":feature:radio"))
    implementation(project(":feature:settings"))
    implementation(project(":core:player"))
    implementation(project(":core:ui"))
    implementation(project(":core:permissions"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    implementation(project(":provider:api"))
    implementation(project(":provider:local"))
    implementation(project(":provider:radio"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.media3.common)
    // RadioBrowserClient/DirectoryCache signatures expose kotlinx.serialization
    // types (Slice H precedent: same alias in :feature:radio).
    implementation(libs.serialization.json)
    // MusicNote/Radio live in the extended set (Slice F precedent); generic M3
    // icons, no custom art (scaffold Req 5).
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
