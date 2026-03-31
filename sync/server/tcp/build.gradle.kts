plugins {
    id(hnau.plugins.hnau.kmp.get().pluginId)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":sync:server:core"))
                api(project(":sync:tcp"))
                implementation(libs.ktor.server.cio)
                implementation(hnau.kotlinx.serialization.cbor)
            }
        }
    }
}