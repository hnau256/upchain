plugins {
    id(hnau.plugins.hnau.kmp.get().pluginId,)
}

kotlin {
    jvm()
    sourceSets {
        jvmMain {
            dependencies {
                api(project(":sync:server:core"))
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.cio)
                implementation(libs.ktor.server.content.negotiation)
                implementation(libs.ktor.serialization.json)
            }
        }
    }
}
