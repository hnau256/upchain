plugins {
    id(hnau.plugins.hnau.kmp.get().pluginId)
}

kotlin {
    jvm()
    sourceSets {
        commonMain {
            dependencies {
                api(project(":sync:server:core"))
                api(project(":sync:http"))
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.cio)
                implementation(libs.ktor.server.content.negotiation)
                implementation(libs.ktor.serialization.json)
            }
        }
    }
}
