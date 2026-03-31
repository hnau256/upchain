plugins {
    id(hnau.plugins.hnau.kmp.get().pluginId)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":sync:core"))
                implementation(libs.ktor.io)
                implementation(hnau.kotlinx.serialization.cbor)
            }
        }
    }
}