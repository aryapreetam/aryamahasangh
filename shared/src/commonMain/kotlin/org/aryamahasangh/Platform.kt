package org.aryamahasangh

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform