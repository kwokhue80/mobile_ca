plugins {
    // 1. Keep the JVM, Serialization, and Kapt plugins using your catalog alias format
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false

    // 2. Map Kapt via alias if it exists in your catalog, or declare it using your version helper cleanly
    id("org.jetbrains.kotlin.kapt") version libs.versions.kotlin.get() apply false

    // 3. Keep the ObjectBox plugin
    alias(libs.plugins.objectbox) apply false
}