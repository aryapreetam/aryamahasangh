package org.aryamahasangh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.vinceglb.filekit.core.FileKit
import org.aryamahasangh.config.AndroidContextHolder
import org.aryamahasangh.config.ConfigInitializer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Android context holder
        AndroidContextHolder.init(this)
        
        // Initialize cross-platform configuration
        ConfigInitializer.initializeBlocking()
        
        // Initialize FileKit
        FileKit.init(this)
        
        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}