rootProject.name = "Upchain"

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

plugins {
    id("org.hnau.plugin.settings") version "1.28.0"
}

hnau {
    publish {
        version = "1.8.0"
        gitUrl = "https://github.com/hnau256/upchain"
    }
}
