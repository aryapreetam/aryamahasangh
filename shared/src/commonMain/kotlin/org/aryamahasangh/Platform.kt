package org.aryamahasangh

enum class Platform {
    ANDROID,
    IOS,
    WEB,
    DESKTOP
}

expect fun getPlatform(): Platform