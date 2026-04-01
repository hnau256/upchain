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
    id("org.hnau.plugin.settings") version "1.9.0"
}

hnau {
    groupId = "org.hnau.upchain"

    publish {
        version = "1.2.0"
        gitUrl = "https://github.com/hnau256/upchain"
    }
}
