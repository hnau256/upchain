plugins {
    id(hnau.plugins.ksp.get().pluginId)
    id(hnau.plugins.hnau.kmp.get().pluginId)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":core"))
                implementation(project(":sync:core"))
                implementation(libs.ktor.server.cio)
                implementation(hnau.kotlinx.serialization.cbor)
            }
        }
    }
}