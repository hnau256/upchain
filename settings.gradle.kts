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
    id("org.hnau.plugin.settings") version "1.11.1"
}

hnau {
    publish {
        version = "1.4.0"
        gitUrl = "https://github.com/hnau256/upchain"
    }
}
