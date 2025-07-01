package com.aryamahasangh.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aryamahasangh.features.admin.FamiliesUiState
import com.aryamahasangh.features.admin.FamilyViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AryaPariwarScreen(familyViewModel: FamilyViewModel? = null) {
  val familiesUiState by familyViewModel?.familiesUiState?.collectAsStateWithLifecycle()
    ?: remember { mutableStateOf(FamiliesUiState()) }

  LaunchedEffect(Unit) {
    familyViewModel?.loadFamilyAndFamilyMemberCount()
  }

  Column(
    modifier = Modifier.padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState()),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(24.dp),
  ) {
    // Header Section
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Spacer(modifier = Modifier.height(0.dp))
      Text(
        text = "आर्य परिवार",
        style = MaterialTheme.typography.headlineMedium.copy(
          fontSize = 32.sp,
          fontWeight = FontWeight.Bold
        ),
        color = MaterialTheme.colorScheme.primary,
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
      text = """
        "आर्य परिवार" वह होता है जहाँ सम्पूर्ण कुटुम्ब आर्य सिद्धांतों को जानकर, उन्हें अपने जीवन में उतारकर, नित्य सन्ध्योपासना करता हुआ आचरण करता है। जब कोई व्यक्ति द्विदिवसीय आर्य प्रशिक्षण सत्र से ज्ञान प्राप्त कर आर्य बनता है और वह स्वयं के साथ-साथ अपने परिवार को भी उन सिद्धांतों पर चलने हेतु प्रेरित करता है, तब ऐसा परिवार "पूर्ण आर्य परिवार" कहलाता है। केवल एक सदस्य आर्य हो — जैसे पति आर्य हो पर पत्नी न हो — तो वह "एकल आर्य" होगा, पूर्ण आर्य परिवार नहीं।
        आर्य परिवारों के समुच्चय से ही सुदृढ़ आर्य समाज की स्थापना होती है। आर्य समाज की दीर्घकालीन सफलता के लिए आर्य परिवारों की अनिवार्यता है। "कृण्वन्तो विश्वमार्यम्" का महान ध्येय तभी पूर्ण होगा जब आर्य -> आर्य परिवार -> आर्य समाज -> आर्य राष्ट्र की शृंखला विकसित हो।
        संवर्धिनी सभा की भूमिका:
        "संवर्धिनी सभा" का प्रमुख कार्य है —
        - आर्य परिवारों का निर्माण, संवर्धन, तथा संरक्षण।
        - उन सभी क्षेत्रों में जहाँ सत्र सम्पन्न हुए हैं, प्रतिभागी आर्यों को परिवार सहित आर्य परिवारों में संगठित करना।
        - यदि कोई एकल आर्य हो तो उसके आर्य परिवार निर्माण हेतु भविष्य में प्रयास करना।
        - आर्य परिवारों को १६ संस्कारों से संयुक्त जीवनशैली हेतु प्रोत्साहित करना।
        - संगठन में सक्रिय किन्तु आर्थिक रूप से दुर्बल आर्य परिवारों की सहायता करना।
      """.trimIndent()
    )

    // Summary Text
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer
      ),
      shape = RoundedCornerShape(16.dp),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "आर्य महासंघ परिवार",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold
          ),
          color = MaterialTheme.colorScheme.onTertiaryContainer
        )

        if (!familiesUiState.isLoading) {
          Text(
            text = "वर्तमान में कुल ${familiesUiState.familyCount} परिवार आर्य महासंघ से ${if(familiesUiState.familyCount > 1) "जुड़े" else "जुडा"} हैं। कुल मिलाकर, इन सभी परिवारों में ${familiesUiState.familyMemberCount} सदस्य हैं।",
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
  }
}
