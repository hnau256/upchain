plugins {
    id(
        hnau.plugins.kotlin.serialization
            .get()
            .pluginId,
    )
    id(
        hnau.plugins.hnau.jvm
            .get()
            .pluginId,
    )
    application
}

dependencies {
    implementation(libs.kotlinx.cli)
    implementation(project(":sync:client:http"))
}

application {
    mainClass = "org.hnau.upchain.sync.client.app.MainKt"
}
