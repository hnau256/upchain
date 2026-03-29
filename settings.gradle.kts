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
    id("org.hnau.plugin.settings") version "1.7.0"
}

hnau {
    groupId = "org.hnau.upchain"

    publish {
        version = "1.0.0"
        gitUrl = "https://github.com/hnau256/upchain"
    }
}
