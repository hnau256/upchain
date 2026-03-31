plugins {
    id(hnau.plugins.hnau.kmp.get().pluginId)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":sync:core"))
                implementation(hnau.kotlinx.serialization.json)
            }
        }
    }
}