plugins {
    application
    id(hnau.plugins.hnau.jvm.get().pluginId)
}

dependencies {
    implementation(libs.kotlinx.cli)
    implementation(project(":sync:server:http"))
}

application {
    mainClass = "org.hnau.upchain.sync.server.app.MainKt"
}