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
    id("org.hnau.plugin.settings") version "1.20.2"
}

hnau {
    publish {
        version = "1.5.1"
        gitUrl = "https://github.com/hnau256/upchain"
    }
}
