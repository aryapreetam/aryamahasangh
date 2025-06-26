package org.aryamahasangh.screens

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
import org.aryamahasangh.components.AddressComponent
import org.aryamahasangh.components.AddressData
import org.aryamahasangh.components.AddressFieldsConfig
import org.aryamahasangh.features.admin.data.AryaSamajListItem
import org.aryamahasangh.features.admin.data.AryaSamajViewModel

@Composable
fun AryaSamajHomeScreen(
  viewModel: AryaSamajViewModel? = null,
  onNavigateToDetail: (String) -> Unit = {}
) {
  val listUiState by viewModel?.listUiState?.collectAsStateWithLifecycle()
    ?: remember { mutableStateOf(org.aryamahasangh.features.admin.data.AryaSamajListUiState()) }

  var addressData by remember { mutableStateOf(AddressData()) }
  var hasSearched by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    viewModel?.loadAryaSamajs()
  }

  LaunchedEffect(addressData) {
    // Only search when user has actually selected something
    if (hasSearched && (addressData.state.isNotEmpty() || addressData.district.isNotEmpty() || addressData.vidhansabha.isNotEmpty())) {
      // For now, just reload all - we can implement filtering later
      viewModel?.loadAryaSamajs()
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
              model = "https://shikshanam.in/wp-content/uploads/2024/09/Firefly-20240902150138-1.png",
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
                text = "आर्य समाज एक हिंदू सुधार आंदोलन है जिसकी स्थापना 1875 में स्वामी दयानंद सरस्वती द्वारा की गई थी। यह संगठन वेदों को सभी ज्ञान का स्रोत मानता है और मूर्ति पूजा, जाति प्रथा तथा अंधविश्वास का विरोध करता है। आर्य समाज शिक्षा, स्वास्थ्य सेवा और समाज सुधार के क्षेत्र में महत्वपूर्ण योगदान देता है। इसका मुख्य उद्देश्य सत्य के प्रकाश से अज्ञानता के अंधकार को दूर करना है।",
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
              model = "https://shikshanam.in/wp-content/uploads/2024/09/Firefly-20240902150138-1.png",
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
                text = "आर्य समाज एक हिंदू सुधार आंदोलन है जिसकी स्थापना 1875 में स्वामी दयानंद सरस्वती द्वारा की गई थी। यह संगठन वेदों को सभी ज्ञान का स्रोत मानता है और मूर्ति पूजा, जाति प्रथा तथा अंधविश्वास का विरोध करता है। आर्य समाज शिक्षा, स्वास्थ्य सेवा और समाज सुधार के क्षेत्र में महत्वपूर्ण योगदान देता है। इसका मुख्य उद्देश्य सत्य के प्रकाश से अज्ञानता के अंधकार को दूर करना है।",
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
                  text = "${listUiState.aryaSamajs.size}",
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
              Text("आर्य समाज दिखाएं")
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
      if (aryaSamaj.formattedAddress.isNotEmpty()) {
        // Placeholder for photos grid (you can add actual photos when available)
        Box(
          modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp)),
          contentAlignment = Alignment.Center
        ) {
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
