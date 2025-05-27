package org.aryamahasangh

import androidx.compose.ui.window.ComposeUIViewController
import org.aryamahasangh.config.ConfigInitializer

fun MainViewController() = ComposeUIViewController { 
    // Initialize cross-platform configuration
    ConfigInitializer.initialize()
    
    App() 
}