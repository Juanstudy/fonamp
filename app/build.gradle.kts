plugins {
    alias(libs.plugins.agp.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

import java.util.Properties

// Release signing: local `keystore.properties` (gitignored) or env vars
// (FONAMP_STORE_FILE/PASSWORD/KEY_ALIAS/KEY_PASSWORD, used by CI).
// Absent both, release builds unsigned.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use(::load)
}
fun signingProp(name: String, env: String): String? =
    keystoreProps.getProperty(name)?.ifBlank { null } ?: System.getenv(env)
val releaseStoreFile = signingProp("storeFile", "FONAMP_STORE_FILE")?.let(::file)
val hasReleaseSigning = releaseStoreFile != null && releaseStoreFile.exists()

android {
    namespace = "com.fonamp.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.fonamp.app"
        minSdk = 29
        targetSdk = 35
        versionCode = 11
        versionName = "0.0.11"
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = releaseStoreFile
                storePassword = signingProp("storePassword", "FONAMP_STORE_PASSWORD")
                keyAlias = signingProp("keyAlias", "FONAMP_KEY_ALIAS")
                keyPassword = signingProp("keyPassword", "FONAMP_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        // v1 ships a direct debug APK: no R8/minify (scaffold Req 6).
        debug {
            isMinifyEnabled = false
        }
        release {
            // Signed when keystore.properties/env is present, unsigned otherwise.
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
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
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
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
    // Generic M3 icons: core set + project-owned AppIcons copies of the
    // six extended-only glyphs (no material-icons-extended dependency).
    implementation("androidx.compose.material:material-icons-core")
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
