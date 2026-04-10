pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
    plugins {
        kotlin("jvm")                                    version "2.0.21"
        kotlin("multiplatform")                          version "2.0.21"
        kotlin("plugin.serialization")                   version "2.0.21"
        id("org.jetbrains.kotlin.plugin.compose")        version "2.0.21"
        id("org.jetbrains.compose")                      version "1.7.0"
    }
}

rootProject.name = "rag-starter"

include(":frontend")
