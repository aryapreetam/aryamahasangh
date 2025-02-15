package org.aryamahasangh.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun JoinUsScreen() {
  Column(modifier = Modifier.padding(16.dp)) {
    Text("नमस्ते जी,\n" +
        "आप निर्मात्री सभा द्वारा आयोजित दो दिवसीय लघु गुरुकुल पाठ्यक्रम पूर्ण कर आर्य महासंघ से जुड़ सकते है। \n" +
        "\n" +
        "निचे आप अपना क्षेत्र चुनकर आपके क्षेत्रों में आयोजित होने वाले सत्रों के विवरण देख सकते है। ")
  }
}
