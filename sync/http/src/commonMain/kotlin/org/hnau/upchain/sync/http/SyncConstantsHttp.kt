package org.hnau.upchain.sync.http

import kotlinx.serialization.json.Json

object SyncConstantsHttp {

    val route: String = "/"

    val json: Json = Json {
        prettyPrint = false
        isLenient = true
        ignoreUnknownKeys = true
    }
}