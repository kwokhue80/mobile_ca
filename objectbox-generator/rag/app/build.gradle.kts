plugins {
    java
    kotlin("jvm")
    kotlin("kapt")
    alias(libs.plugins.kotlin.serialization) // Registers JSON mapping tasks cleanly
    alias(libs.plugins.objectbox)
}


sourceSets {
    getByName("main") {
        java.setSrcDirs(listOf("src/main/java", "src/main/kotlin"))
    }
}

dependencies {
    // Standard notation accessor mapping
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.negotiation)
    implementation(libs.ktor.serialization)
    implementation(libs.langchain4j.embeddings)
    implementation(libs.langchain4j.core)
    implementation(libs.langchain4j.embeddings)
}