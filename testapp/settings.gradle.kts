pluginManagement {
    val springBootVersion: String by settings
    val springDependencyManagementVersion: String by settings
    val avroPluginVersion: String by settings

    plugins {
        id("org.springframework.boot") version springBootVersion
        id("io.spring.dependency-management") version springDependencyManagementVersion
        id("com.github.davidmc24.gradle.plugin.avro") version avroPluginVersion
    }
}

rootProject.name = "testtask"
