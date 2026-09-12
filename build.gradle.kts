// Slice A: Gradle 8.10 + AGP 8.7 toolchain (scaffold Req 6).
// Root holds toolchain only; versions live in gradle/libs.versions.toml.
plugins {
    alias(libs.plugins.agp.application) apply false
    alias(libs.plugins.agp.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}
