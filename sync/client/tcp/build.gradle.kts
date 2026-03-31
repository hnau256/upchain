plugins {
    id(hnau.plugins.hnau.kmp.get().pluginId)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":sync:client:core"))
                implementation(libs.ktor.client.cio)
                implementation(hnau.kotlinx.serialization.cbor)
            }
        }
    }
}