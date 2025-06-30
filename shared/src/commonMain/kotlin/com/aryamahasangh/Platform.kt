package com.aryamahasangh

enum class Platform {
  ANDROID,
  IOS,
  WEB,
  DESKTOP
}

fun isAndroid() = getPlatform() == Platform.ANDROID

fun isWeb() = getPlatform() == Platform.WEB

fun isIos() = getPlatform() == Platform.IOS

fun isDesktop() = getPlatform() == Platform.DESKTOP

expect fun getPlatform(): Platform
