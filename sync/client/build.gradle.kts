plugins {
    id(hnau.plugins.hnau.kmp.get().pluginId)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":core"))
                implementation(project(":sync:core"))
                implementation(libs.ktor.client.cio)
                implementation(hnau.kotlinx.serialization.cbor)
            }
        }
    }
}