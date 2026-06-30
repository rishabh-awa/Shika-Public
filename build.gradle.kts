// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
    // 🚀 EMBED THE PYTHON RUNTIME ENVIRONMENT
    id("com.chaquo.python") version "17.0.0" apply false
}