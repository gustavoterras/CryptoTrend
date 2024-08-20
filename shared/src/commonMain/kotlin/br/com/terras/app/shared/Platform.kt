package br.com.terras.app.shared

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform