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
                implementation(libs.ktor.server.cio)
                implementation(hnau.kotlinx.serialization.json)
            }
        }
    }
}
