package org.hnau.upchain.sync.http

enum class HttpScheme {
    Http, Https;

    companion object {

        val default: HttpScheme
            get() = Https
    }
}