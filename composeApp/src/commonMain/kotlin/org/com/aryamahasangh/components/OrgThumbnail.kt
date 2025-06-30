package com.aryamahasangh.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aryamahasangh.LocalIsAuthenticated
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun OrgThumbnailPreview() {
  OrgItem(
    "राष्ट्रीय आर्य संवर्धिनी सभा",
    "राष्ट्रीय आर्य संवर्धिनी सभा  एकमात्र वह संस्था है जो आर्य परिवारों के निर्माण, संरक्षण और उनके संवर्धन के लिए सदा प्रयासरत है। इस सभा की स्थापना आश्विन मास, शुक्लपक्ष, दशमी तिथि, विजयादशमी पर्व तदनुसार 24 अक्टूबर 2023 को आर्यसमाज शिवाजी कालोनी, रोहतक हरियाणा में सम्पन्न हुई थी। यह सभा आर्य महासंघ के अन्तर्गत  और उसके उद्देश्य अनुसार गतिविधियों को सम्पन्न करने के लिए बनाई गई है। वर्तमान में इस सभा के राष्ट्रीय अध्यक्ष आचार्य वर्चस्पति हिसार, राष्ट्रीय उपाध्यक्ष आर्य वेदप्रकाश रोहतक, राष्ट्रीय महासचिव आचार्य चरण सिंह भरतपुर , राष्ट्रीय कोषाध्यक्ष आर्य वेद गुरुग्राम, हरियाणा प्रान्त सचिव आर्य मनीराम, दिल्ली प्रान्त अध्यक्ष आचार्य राजेश और सचिव आर्य कप्तान, उत्तर प्रदेश प्रान्त अध्यक्ष आर्य भारत शास्त्री और सचिव आर्य रोबिन को मनोनीत किया गया है। इस सभा का राष्ट्रीय कार्यालय क्षात्र गुरुकुल, भाली आनन्दपुर, रोहतक में स्थित है। किसी भी देश, राष्ट्र और समाज की प्रथम इकाई परिवार ही होती है और परिवार श्रेष्ठ अर्थात् आर्य होना चाहिए, इसी उद्देश्य की पूर्ति के लिए यह सभा और इसके कार्यकर्ता अहर्निश कार्यरत हैं, धन्यवाद!",
    {},
    {}
  )
}

@Composable
fun OrgItem(
  name: String,
  description: String,
  navigateToOrgDetails: () -> Unit,
  onDeleteOrganisation: () -> Unit = {}
) {
  val isLoggedIn = LocalIsAuthenticated.current
  var showConfirmDialog by remember { mutableStateOf(false) }
  var showMenu by remember { mutableStateOf(false) }

  ElevatedCard(
    onClick = navigateToOrgDetails,
    modifier = Modifier.width(500.dp),
    shape = RoundedCornerShape(4.dp)
  ) {
    Box(
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp)) {
        Text(
          text = name,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = description,
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }
      // Only show delete option for logged-in users and exclude "आर्य महासंघ"
      if (isLoggedIn) {
        Box(
          modifier = Modifier.align(Alignment.TopEnd)
        ) {
          IconButton(
            onClick = { showMenu = true }
          ) {
            Icon(
              Icons.Default.MoreVert,
              contentDescription = "More options",
              Modifier.size(24.dp)
            )
          }
          DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
          ) {
            DropdownMenuItem(
              text = { Text("संस्था हटाएँ") },
              onClick = {
                showMenu = false
                showConfirmDialog = true
              },
              leadingIcon = {
                Icon(
                  Icons.Default.Delete,
                  contentDescription = "Delete",
                  Modifier.size(24.dp)
                )
              }
            )
          }
        }
      }
    }
  }

  // Confirmation dialog
  if (showConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showConfirmDialog = false },
      title = { Text("संस्था हटाएँ") },
      text = { Text("क्या आप निश्चितरूप से इस संस्था को हटाना चाहते हैं?") },
      confirmButton = {
        TextButton(onClick = {
          showConfirmDialog = false
          onDeleteOrganisation()
        }) {
          Text("हाँ")
        }
      },
      dismissButton = {
        TextButton(onClick = { showConfirmDialog = false }) {
          Text("नहीं")
        }
      }
    )
  }
}
