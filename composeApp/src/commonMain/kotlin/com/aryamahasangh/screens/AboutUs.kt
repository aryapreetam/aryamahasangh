package com.aryamahasangh.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import aryamahasangh.composeapp.generated.resources.Res
import aryamahasangh.composeapp.generated.resources.mahasangh_logo_without_background
import com.aryamahasangh.LocalIsAuthenticated
import com.aryamahasangh.components.LoadingErrorState
import com.aryamahasangh.navigation.Screen
import com.aryamahasangh.repository.OrganisationName
import com.aryamahasangh.utils.WithTooltip
import com.aryamahasangh.viewmodel.AboutUsViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun AboutUs(
  showDetailedAboutUs: (String) -> Unit,
  viewModel: AboutUsViewModel,
  navigateToScreen: (Screen) -> Unit = {}
) {
  val uiState by viewModel.uiState.collectAsState()
  val isAuthenticated = LocalIsAuthenticated.current

  LoadingErrorState(
    isLoading = uiState.isLoading,
    error = uiState.appError,
    onRetry = {
      viewModel.clearError()
      viewModel.loadOrganisationDetails("आर्य महासंघ")
    },
    loadingContent = {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        CircularProgressIndicator()
      }
    }
  ) {
    LazyColumn(
      modifier = Modifier.padding(8.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Organization Header Section
      item {
        Column(
          modifier = Modifier.fillMaxWidth().clickable {
            uiState.organisation?.id?.let { id ->
              showDetailedAboutUs(id)
            }
          },
          verticalArrangement = Arrangement.spacedBy(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Image(
            painter = painterResource(resource = Res.drawable.mahasangh_logo_without_background),
            contentDescription = "logo आर्य महासंघ",
            modifier = Modifier.size(128.dp)
          )
          Text(
            modifier = Modifier
              .semantics { contentDescription = "about_intro_paragraph" }
              .testTag("about_intro_paragraph"),
            text = uiState.organisation?.description
              ?: "सनातन धर्म का साक्षात् प्रतिनिधि 'आर्य' ही होता है। आर्य ही धर्म को जीता है, समाज को मर्यादाओं में बांधता है और राष्ट्र को सम्पूर्ण भू-मण्डल में प्रतिष्ठित करता है। आर्य के जीवन में अनेकता नहीं एकता रहती है अर्थात् एक ईश्वर, एक धर्म, एक धर्मग्रन्थ और एक उपासना पद्धति। ऐसे आर्यजन लाखों की संख्या में मिलकर संगठित, सुव्यवस्थित और सुनियोजित रीति से आगे बढ़ रहे हैं - आर्यावर्त की ओर--- यही है - आर्य महासंघ ।।"
          )
        }
      }

      // Quick Links Section
      item {
        QuickLinksSection(
          navigateToScreen = navigateToScreen,
          isAuthenticated = isAuthenticated,
          organisationNames = uiState.organisationNames
        )
      }
    }
  }
}

@Composable
private fun QuickLinksSection(
  navigateToScreen: (Screen) -> Unit,
  isAuthenticated: Boolean,
  organisationNames: List<OrganisationName>
) {
  // Section Header
  Text(
    text = "उपलब्ध सुविधाएँ",
    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(vertical = 8.dp)
  )
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // गतिविधियां Section
    QuickLinkGroup(
      title = "आर्य गतिविधियां",
      icon = Icons.Default.Event,
      items = listOf(
        QuickLinkItem(
          "आर्य प्रशिक्षण सत्र",
          "आर्य प्रशिक्षण",
          Screen.Activities("आर्य प्रशिक्षण सत्र")
        ),
        QuickLinkItem("बोध सत्र", "धार्मिक ज्ञान वृद्धि हेतु सत्र", Screen.Activities("बोध सत्र")),
        QuickLinkItem("क्षात्र प्रशिक्षण", "क्षात्र प्रशिक्षण", Screen.Activities("क्षात्र प्रशिक्षण")),
        QuickLinkItem(
          "आर्य वीरांगना प्रशिक्षण",
          "वीरांगना प्रशिक्षण",
          Screen.Activities("आर्य वीरांगना प्रशिक्षण")
        ),
        QuickLinkItem("अभियान", "अभियान", Screen.Activities("अभियान")),
        QuickLinkItem("कक्षा", "कक्षा", Screen.Activities("कक्षा")),
        QuickLinkItem("कार्यक्रम", "कार्यक्रम", Screen.Activities("कार्यक्रम"))
      ),
      navigateToScreen = navigateToScreen,
      cardColor = CardBackgroundColor.Blue
    )

    // संलग्न संस्थाएं Section
    QuickLinkGroup(
      title = "संलग्न आर्य संस्थाएं",
      icon = Icons.Default.Business,
      items = organisationNames.map { org ->
        QuickLinkItem(
          title = org.name,
          description = org.name, // Using name as description as requested
          screen = Screen.OrgDetails(org.id)
        )
      },
      navigateToScreen = navigateToScreen,
      cardColor = CardBackgroundColor.Green
    )

    // आर्य गुरुकुल Section
    QuickLinkGroup(
      title = "आर्य गुरुकुल महाविद्यालय",
      icon = Icons.Default.School,
      items = emptyList(),
      navigateToScreen = navigateToScreen,
      cardColor = CardBackgroundColor.Purple,
      subSections = listOf(
        QuickLinkSubSection(
          title = "आर्य गुरुकुल",
          items = listOf(
//            QuickLinkItem("वर्तमान कार्य", "चालू गतिविधियां", Screen.AryaGurukulCollege),
//            QuickLinkItem("गुरुकुल प्रवेश", "नए छात्रों का प्रवेश", Screen.AryaGurukulCollege),
//            QuickLinkItem("आगामी बैठक", "भविष्य की बैठकें", Screen.AryaGurukulCollege),
            QuickLinkItem("कक्षाएं", "शिक्षा कार्यक्रम", Screen.AryaGurukulCollege)
          )
        ),
        QuickLinkSubSection(
          title = "आर्या गुरुकुल",
          items = listOf(
//            QuickLinkItem("वर्तमान कार्य", "चालू गतिविधियां", Screen.AryaaGurukulCollege),
//            QuickLinkItem("गुरुकुल प्रवेश", "नए छात्राओं का प्रवेश", Screen.AryaaGurukulCollege),
//            QuickLinkItem("आगामी बैठक", "भविष्य की बैठकें", Screen.AryaaGurukulCollege),
            QuickLinkItem("कक्षाएं", "शिक्षा कार्यक्रम", Screen.AryaaGurukulCollege)
          )
        )
      )
    )

    // संगठन Section
    QuickLinkGroup(
      title = "आर्य संगठन",
      icon = Icons.Default.Groups,
      items = listOf(
        QuickLinkItem("सत्र पंजीकरण", "कार्यक्रमों में भाग लेने हेतु", Screen.AryaNirmanHome),
        QuickLinkItem("आर्य परिवार", "पारिवारिक पंजीकरण", Screen.AryaPariwarHome),
        QuickLinkItem("आर्य समाज संगठन", "स्थानीय समाज", Screen.AryaSamajHome),
//        QuickLinkItem("क्षात्र शिविर पंजीकरण", "प्रशिक्षण शिविर", Screen.KshatraTrainingHome),
//        QuickLinkItem("छात्रा शिविर पंजीकरण", "महिला प्रशिक्षण", Screen.ChatraTrainingHome)
      ),
      navigateToScreen = navigateToScreen,
      cardColor = CardBackgroundColor.Gray
    )

    // स्वाध्याय Section
//    QuickLinkGroup(
//      title = "स्वाध्याय",
//      icon = Icons.Default.MenuBook,
//      items = listOf(
//        QuickLinkItem("सत्यार्थ प्रकाश", "महर्षि दयानंद का मुख्य ग्रंथ", Screen.Learning)
//      ),
//      navigateToScreen = navigateToScreen,
//      cardColor = CardBackgroundColor.Teal
//    )

    // व्यवस्थापकीय Section (Only for authenticated users)
    if (isAuthenticated) {
      QuickLinkGroup(
        title = "व्यवस्थापकीय",
        icon = Icons.Default.AdminPanelSettings,
        items = listOf(
          QuickLinkItem("नया आर्य जोड़ें", "नए सदस्य का पंजीकरण", Screen.AddMemberForm),
          QuickLinkItem("नया आर्य समाज जोड़ें", "नई शाखा का पंजीकरण", Screen.AddAryaSamajForm),
          QuickLinkItem("नया परिवार जोड़ें", "पारिवारिक पंजीकरण", Screen.CreateFamilyForm)
        ),
        navigateToScreen = navigateToScreen,
        cardColor = CardBackgroundColor.Indigo,
        subSections = listOf(
          QuickLinkSubSection(
            title = "विवरण अद्यतन",
            items = listOf(
              QuickLinkItem("आर्य विवरण अद्यतन", "सदस्य जानकारी संपादन", Screen.AdminContainer(3)),
              QuickLinkItem("आर्य समाज विवरण अद्यतन", "शाखा विवरण संपादन", Screen.AdminContainer(1)),
              QuickLinkItem("आर्य परिवार अद्यतन", "पारिवारिक विवरण संपादन", Screen.AdminContainer(2))
            )
          )
        )
      )
    }
  }
}

// Enum for card background colors
private enum class CardBackgroundColor {
  Blue, Green, Purple, Gray, Teal, Indigo
}

@Composable
private fun CardBackgroundColor.toComposeColor(): androidx.compose.ui.graphics.Color {
  return when (this) {
    CardBackgroundColor.Blue -> androidx.compose.ui.graphics.Color(0xFF64B5F6).copy(alpha = 0.15f)      // Light Blue
    CardBackgroundColor.Green -> androidx.compose.ui.graphics.Color(0xFF81C784).copy(alpha = 0.15f)     // Light Green
    CardBackgroundColor.Purple -> androidx.compose.ui.graphics.Color(0xFFBA68C8).copy(alpha = 0.15f)    // Light Purple
    CardBackgroundColor.Gray -> androidx.compose.ui.graphics.Color(0xFF90A4AE).copy(alpha = 0.15f)      // Light Gray
    CardBackgroundColor.Teal -> androidx.compose.ui.graphics.Color(0xFF4DB6AC).copy(alpha = 0.15f)      // Light Teal
    CardBackgroundColor.Indigo -> androidx.compose.ui.graphics.Color(0xFF7986CB).copy(alpha = 0.15f)    // Light Indigo
  }
}

@Composable
private fun CardBackgroundColor.toTextColor(): androidx.compose.ui.graphics.Color {
  return when (this) {
    CardBackgroundColor.Blue -> androidx.compose.ui.graphics.Color(0xFF1976D2)      // Darker Blue
    CardBackgroundColor.Green -> androidx.compose.ui.graphics.Color(0xFF388E3C)     // Darker Green
    CardBackgroundColor.Purple -> androidx.compose.ui.graphics.Color(0xFF7B1FA2)    // Darker Purple
    CardBackgroundColor.Gray -> androidx.compose.ui.graphics.Color(0xFF455A64)      // Darker Gray
    CardBackgroundColor.Teal -> androidx.compose.ui.graphics.Color(0xFF00695C)      // Darker Teal
    CardBackgroundColor.Indigo -> androidx.compose.ui.graphics.Color(0xFF303F9F)    // Darker Indigo
  }
}

@Composable
private fun QuickLinkGroup(
  title: String,
  icon: ImageVector,
  items: List<QuickLinkItem>,
  navigateToScreen: (Screen) -> Unit,
  cardColor: CardBackgroundColor,
  subSections: List<QuickLinkSubSection> = emptyList()
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(4.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Group Header
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp)
        )
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.primary
        )
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))

      // Main Items
      if (items.isNotEmpty()) {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
          items(items) { item ->
            QuickLinkCard(
              item = item,
              onClick = { navigateToScreen(item.screen) },
              cardColor = cardColor
            )
          }
        }
      }

      // Sub Sections
      subSections.forEach { subSection ->
        Column(
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "• ${subSection.title}",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(start = 8.dp)
          )

          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
          ) {
            items(subSection.items) { item ->
              QuickLinkCard(
                item = item,
                onClick = { navigateToScreen(item.screen) },
                cardColor = cardColor
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun QuickLinkCard(
  item: QuickLinkItem,
  onClick: () -> Unit,
  cardColor: CardBackgroundColor
) {
  WithTooltip(tooltip = item.description) {
    Card(
      modifier = Modifier
        .height(48.dp)
        .clickable { onClick() },
      shape = RoundedCornerShape(4.dp),
      colors = CardDefaults.cardColors(
        containerColor = cardColor.toComposeColor()
      ),
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(8.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          modifier = Modifier.padding(horizontal = 12.dp),
          text = item.title,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
          color = cardColor.toTextColor(),
          maxLines = 3,
          textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
      }
    }
  }
}

// Data classes for Quick Links
private data class QuickLinkItem(
  val title: String,
  val description: String,
  val screen: Screen
)

private data class QuickLinkSubSection(
  val title: String,
  val items: List<QuickLinkItem>
)

@Preview
@Composable
fun AboutUsIntro() {
  // This is just a preview, so we don't need a real ViewModel
  // In a real app, we would inject the ViewModel
  // AboutUs(showDetailedAboutUs = {}, viewModel = AboutUsViewModel())
}
