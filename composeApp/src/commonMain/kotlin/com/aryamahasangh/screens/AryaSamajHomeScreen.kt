package com.aryamahasangh.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.aryamahasangh.components.AddressComponent
import com.aryamahasangh.components.AddressData
import com.aryamahasangh.components.AddressFieldsConfig
import com.aryamahasangh.features.admin.data.AryaSamajListItem
import com.aryamahasangh.features.admin.data.AryaSamajListUiState
import com.aryamahasangh.features.admin.data.AryaSamajViewModel

@Composable
fun AryaSamajHomeScreen(
  viewModel: AryaSamajViewModel? = null,
  onNavigateToDetail: (String) -> Unit = {}
) {
  val listUiState by viewModel?.listUiState?.collectAsStateWithLifecycle()
    ?: remember { mutableStateOf(AryaSamajListUiState()) }

  var addressData by remember { mutableStateOf(AddressData()) }
  var hasSearched by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    viewModel?.loadAryaSamajs()
    viewModel?.getAryaSamajCount() // Load total count separately
  }

  LaunchedEffect(addressData) {
    // Only search when user has actually selected something
    if (hasSearched && (addressData.state.isNotEmpty() || addressData.district.isNotEmpty() || addressData.vidhansabha.isNotEmpty())) {
      // Call search with address parameters
      viewModel?.searchAryaSamajByAddress(
        state = addressData.state.ifBlank { null },
        district = addressData.district.ifBlank { null },
        vidhansabha = addressData.vidhansabha.ifBlank { null }
      )
    }
  }

  BoxWithConstraints {
    val isCompact = maxWidth < 600.dp

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      item {
        // Header section with photo and description - responsive layout
        if (isCompact) {
          // Compact layout: Photo on top, description below
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            // Maharshi Dayanand photo
            AsyncImage(
              model = "https://placeholder-dev-supabase.co/storage/v1/object/public/images//rishi_dayanand.webp",
              contentDescription = "महर्षि दयानंद सरस्वती",
              modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(12.dp)),
              contentScale = ContentScale.Crop
            )

            // Description
            Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = "आर्य समाज",
                style = MaterialTheme.typography.headlineMedium.copy(
                  fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
              )

              Text(
                text = """
                  जब हम आर्य बना रहे होते हैं तो राष्ट्र ही बना रहे होते हैं। जब हम आर्यसमाज बना रहे होते हैं तो राष्ट्र ही बना रहे होते हैं।
                  यह राजनीति नही है अपितु राष्ट्र निर्माण की वह प्रकिया है जो इसे एक राष्ट्र बनायेगी। यहाँ यह निर्देश संक्षिप्त से इस लिए लिखा है कि जो राष्ट्र के लिए बलिदान को सब कुछ समझते हैं वे इस आर्यसमाज निर्माण को उससे कमतर न समझें अपितु यह उसमे बढ़कर है यह राष्ट्र की आधार शिला है, बिना इस के राष्ट्र का अस्तित्व नही है। इसलिए इस महान कार्य को भी त्याग, बलिदान, पवित्रता एवं महान प्रयास से पूर्ण करें। यह राष्ट्र की वह अप्रतिम सेवा है जो कई हजार वर्ष कि रिक्तता को भरेगा ।
                  साभार :- समाज पुस्तक (आर्य परमदेव जी मीमांसक)
                """.trimIndent(),
                style = MaterialTheme.typography.bodyLarge.copy(
                  lineHeight = 24.sp
                ),
                textAlign = TextAlign.Justify
              )
            }
          }
        } else {
          // Wide layout: Photo on left, description on right
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
          ) {
            // Maharshi Dayanand photo
            AsyncImage(
              model = "https://placeholder-dev-supabase.co/storage/v1/object/public/images//rishi_dayanand.webp",
              contentDescription = "महर्षि दयानंद सरस्वती",
              modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(12.dp)),
              contentScale = ContentScale.Crop
            )

            // Description
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = "आर्य समाज",
                style = MaterialTheme.typography.headlineMedium.copy(
                  fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
              )

              Text(
                text = """
                  जब हम आर्य बना रहे होते हैं तो राष्ट्र ही बना रहे होते हैं। जब हम आर्यसमाज बना रहे होते हैं तो राष्ट्र ही बना रहे होते हैं।
                  यह राजनीति नही है अपितु राष्ट्र निर्माण की वह प्रकिया है जो इसे एक राष्ट्र बनायेगी। यहाँ यह निर्देश संक्षिप्त से इस लिए लिखा है कि जो राष्ट्र के लिए बलिदान को सब कुछ समझते हैं वे इस आर्यसमाज निर्माण को उससे कमतर न समझें अपितु यह उसमे बढ़कर है यह राष्ट्र की आधार शिला है, बिना इस के राष्ट्र का अस्तित्व नही है। इसलिए इस महान कार्य को भी त्याग, बलिदान, पवित्रता एवं महान प्रयास से पूर्ण करें। यह राष्ट्र की वह अप्रतिम सेवा है जो कई हजार वर्ष कि रिक्तता को भरेगा ।
                  साभार :- समाज पुस्तक (आर्य परमदेव जी मीमांसक)
                """.trimIndent(),
                style = MaterialTheme.typography.bodyLarge.copy(
                  lineHeight = 24.sp
                ),
                textAlign = TextAlign.Justify
              )
            }
          }
        }
      }

      item {
        // Total count section
        Card(
          modifier = Modifier.width(500.dp).padding(vertical = 12.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
          )
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = "आर्य समाज(आर्य महासंघ के अंतर्गत)",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.SemiBold
                )
              )

              if (listUiState.isLoading) {
                CircularProgressIndicator(
                  modifier = Modifier.size(32.dp)
                )
              } else {
                Text(
                  text = "${listUiState.totalCount}",
                  style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold
                  ),
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }
          }
        }
      }

      item {
        // Search section
        Column(
          modifier = Modifier.padding(top = 12.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Text(
            text = "निचे अपना क्षेत्र चुनकर निकटवर्ती आर्य समाज देखें",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.SemiBold
            )
          )

          AddressComponent(
            addressData = addressData,
            onAddressChange = { newData ->
              addressData = newData
              hasSearched = true
            },
            fieldsConfig = AddressFieldsConfig(
              showLocation = false,
              showAddress = false,
              showPincode = false,
              showState = true,
              showDistrict = true,
              showVidhansabha = true
            ),
            modifier = Modifier.fillMaxWidth()
          )

          if (hasSearched && (addressData.state.isNotEmpty() || addressData.district.isNotEmpty() || addressData.vidhansabha.isNotEmpty())) {
            Button(
              onClick = {
                addressData = AddressData()
                hasSearched = false
                viewModel?.loadAryaSamajs()
              },
              modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
              Text("सभी आर्य समाज दिखाएं")
            }
          }
        }
      }

      item {
        // Divider
        HorizontalDivider(
          modifier = Modifier
            .fillMaxWidth()
        )
      }

      item {
        // Results section - show all Arya Samaj
        Column(
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Text(
            text = if (hasSearched && (addressData.state.isNotEmpty() || addressData.district.isNotEmpty() || addressData.vidhansabha.isNotEmpty())) {
              "खोज परिणाम"
            } else {
              "आर्य समाज"
            },
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.SemiBold
            )
          )

          if (listUiState.isLoading) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
              contentAlignment = Alignment.Center
            ) {
              CircularProgressIndicator()
            }
          } else if (listUiState.error != null) {
            Card(
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
              )
            ) {
              Text(
                text = listUiState.error ?: "Error occurred",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onErrorContainer
              )
            }
          } else if (listUiState.aryaSamajs.isEmpty()) {
            Card(
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
              )
            ) {
              Text(
                text = if (hasSearched) "इस क्षेत्र में कोई आर्य समाज नहीं मिला" else "कोई आर्य समाज उपलब्ध नहीं है",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
              )
            }
          } else {
            Column(
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              listUiState.aryaSamajs.forEach { aryaSamaj ->
                AryaSamajListItemComponent(
                  aryaSamaj = aryaSamaj,
                  onItemClick = { onNavigateToDetail(aryaSamaj.id) }
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AryaSamajListItemComponent(
  aryaSamaj: AryaSamajListItem,
  onItemClick: () -> Unit
) {
  Card(
    onClick = onItemClick,
    modifier = Modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      verticalAlignment = Alignment.Top
    ) {
      // Photos section (left side)
      Box(
        modifier = Modifier
          .size(80.dp)
          .clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
      ) {
        if (aryaSamaj.mediaUrls.isNotEmpty()) {
          // Show the first image if available
          AsyncImage(
            model = aryaSamaj.mediaUrls.first(),
            contentDescription = aryaSamaj.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
          )
        } else {
          // Fallback to placeholder with name initials
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxSize()
          ) {
            Box(
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = aryaSamaj.name.take(2),
                style = MaterialTheme.typography.titleLarge.copy(
                  fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }
          }
        }
      }

      // Content section (right side)
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = aryaSamaj.name,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold
          ),
          color = MaterialTheme.colorScheme.primary
        )

        if (aryaSamaj.formattedAddress.isNotEmpty()) {
          Text(
            text = aryaSamaj.formattedAddress,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        if (aryaSamaj.description.isNotEmpty()) {
          Text(
            text = aryaSamaj.description,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}
