package org.aryamahasangh.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.aryamahasangh.features.admin.FamilyViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AryaPariwarScreen(familyViewModel: FamilyViewModel? = null) {
  val familiesUiState by familyViewModel?.familiesUiState?.collectAsStateWithLifecycle()
    ?: remember { mutableStateOf(org.aryamahasangh.features.admin.FamiliesUiState()) }

  LaunchedEffect(Unit) {
    familyViewModel?.loadFamilyAndFamilyMemberCount()
  }

  Column(
    modifier = Modifier.padding(16.dp).fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(24.dp)
  ) {
    // Header Section
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Text(
        text = "आर्य परिवार",
        style = MaterialTheme.typography.headlineMedium.copy(
          fontSize = 32.sp,
          fontWeight = FontWeight.Bold
        ),
        color = MaterialTheme.colorScheme.primary
      )
      Text(
        text = "(आर्य राष्ट्र की आधारशिला)",
        style = MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.SemiBold
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    // Description
    Text(
      text = "आर्य परिवार एक उन्नत समाज की मुख्य इकाई है। परिवार ही वह स्थान है जहाँ संस्कार, मूल्य और परंपराओं का स्थानांतरण होता है।",
      style = MaterialTheme.typography.bodyLarge.copy(
        lineHeight = 24.sp
      ),
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(horizontal = 16.dp)
    )

    // Count Cards Section
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Family Count Card
      Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "कुल परिवार",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )

          if (familiesUiState.isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(24.dp),
              color = MaterialTheme.colorScheme.primary
            )
          } else {
            Text(
              text = "${familiesUiState.familyCount}",
              style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold
              ),
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
      }

      // Family Members Count Card
      Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "कुल सदस्य",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSecondaryContainer
          )

          if (familiesUiState.isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(24.dp),
              color = MaterialTheme.colorScheme.secondary
            )
          } else {
            Text(
              text = "${familiesUiState.familyMemberCount}",
              style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold
              ),
              color = MaterialTheme.colorScheme.secondary
            )
          }
        }
      }
    }

    // Summary Text
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer
      ),
      shape = RoundedCornerShape(16.dp),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "आर्य महासंघ परिवार सांख्यिकी",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold
          ),
          color = MaterialTheme.colorScheme.onTertiaryContainer
        )

        if (!familiesUiState.isLoading) {
          Text(
            text = "वर्तमान में कुल ${familiesUiState.familyCount} परिवार आर्य महासंघ से जुड़े हैं। कुल मिलाकर, इन सभी परिवारों में ${familiesUiState.familyMemberCount} सदस्य हैं।",
            style = MaterialTheme.typography.bodyLarge.copy(
              lineHeight = 22.sp
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onTertiaryContainer
          )
        } else {
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              color = MaterialTheme.colorScheme.tertiary
            )
            Text(
              text = "डेटा लोड हो रहा है...",
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onTertiaryContainer
            )
          }
        }

        if (familiesUiState.error != null) {
          Text(
            text = familiesUiState.error ?: "Error loading data",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
          )
        }
      }
    }
  }
}
