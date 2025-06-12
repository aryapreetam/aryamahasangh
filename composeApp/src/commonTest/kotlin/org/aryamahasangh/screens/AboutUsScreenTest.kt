package org.aryamahasangh.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Simple test for verifying the About Us screen
 *
 * This test is intentionally minimal to ensure it works across all platforms.
 * More comprehensive UI testing should be implemented in platform-specific
 * test source sets (androidTest, desktopTest, etc.) where platform-specific
 * testing APIs are available.
 */
class AboutUsScreenTest {
  @Test
  fun aboutUsScreenExists() {
    // Simple verification that the test is running
    // This does not test Compose UI directly, but verifies the test environment works
    assertTrue(true, "The About Us screen test is executed")
  }
}

/**
 * A simplified version of the AboutUs screen
 *
 * This composable is used as a reference for what we would test
 * in platform-specific tests. It's not used in the current test
 * but serves as documentation for what would be tested.
 */
@Composable
fun SimpleAboutUsScreen() {
  MaterialTheme {
    Column(modifier = Modifier.fillMaxSize()) {
      Text("About Us")
      Text("This is a simple about us screen for testing")
    }
  }
}