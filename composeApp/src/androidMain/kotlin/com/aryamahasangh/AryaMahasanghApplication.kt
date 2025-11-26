package com.aryamahasangh

import android.app.Application
import com.aryamahasangh.network.initializeSentry

class AryaMahasanghApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    // Initialize Apollo Client
    initializeSentry()
  }
}
