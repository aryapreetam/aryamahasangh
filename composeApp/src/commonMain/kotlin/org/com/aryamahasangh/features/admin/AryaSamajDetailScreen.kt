package com.aryamahasangh.features.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aryamahasangh.LocalIsAuthenticated
import com.aryamahasangh.features.activities.Member
import com.aryamahasangh.features.admin.data.AryaSamajDetail
import com.aryamahasangh.features.admin.data.AryaSamajMember
import com.aryamahasangh.features.admin.data.AryaSamajViewModel
import com.aryamahasangh.navigation.LocalSnackbarHostState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AryaSamajDetailScreen(
  aryaSamajId: String,
  viewModel: AryaSamajViewModel,
  onNavigateBack: () -> Unit,
  onOpenInMaps: (Double, Double) -> Unit = { _, _ -> },
  searchMembers: (String) -> List<Member> = { emptyList() },
  allMembers: List<Member> = emptyList(),
  onTriggerSearch: (String) -> Unit = {}
) {
  val isAuthenticated = LocalIsAuthenticated.current
  val snackbarHostState = LocalSnackbarHostState.current
  val detailUiState by viewModel.detailUiState.collectAsState()

  LaunchedEffect(aryaSamajId) {
    viewModel.loadAryaSamajDetail(aryaSamajId)
  }

  // Handle loading state
  if (detailUiState.isLoading) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      CircularProgressIndicator()
    }
    return
  }

  // Handle error state
  detailUiState.error?.let { error ->
    LaunchedEffect(error) {
      snackbarHostState.showSnackbar(error)
    }

    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text("आर्य समाज की जानकारी लोड नहीं हो सकी")
        Button(onClick = { viewModel.loadAryaSamajDetail(aryaSamajId) }) {
          Text("पुनः प्रयास करें")
        }
      }
    }
    return
  }

  val aryaSamaj = detailUiState.aryaSamaj
  if (aryaSamaj == null) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Text("आर्य समाज नहीं मिला")
    }
    return
  }

  LazyColumn(
    modifier =
      Modifier
        .fillMaxSize()
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header with name and edit button
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = aryaSamaj.name,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )

        if (isAuthenticated) {
          Row {
            // Temporarily hidden - Edit button
            /*
            IconButton(onClick = { /* TODO: Edit functionality */ }) {
              Icon(
                Icons.Default.Edit,
                contentDescription = "संपादित करें"
              )
            }
            */
            IconButton(onClick = onNavigateBack) {
              Icon(
                Icons.Default.Close,
                contentDescription = "बंद करें"
              )
            }
          }
        } else {
          IconButton(onClick = onNavigateBack) {
            Icon(
              Icons.Default.Close,
              contentDescription = "बंद करें"
            )
          }
        }
      }
    }

    // Address section
    item {
      AddressSection(
        aryaSamaj = aryaSamaj,
        onOpenInMaps = onOpenInMaps
      )
    }

    // Description section
    item {
      DescriptionSection(
        description = aryaSamaj.description,
        isEditable = isAuthenticated
      )
    }

    // Pictures section
    if (aryaSamaj.mediaUrls.isNotEmpty()) {
      item {
        PicturesSection(mediaUrls = aryaSamaj.mediaUrls)
      }
    }

    // Members section
    if (aryaSamaj.members.isNotEmpty()) {
      item {
        MembersSection(
          members = aryaSamaj.members,
          isEditable = isAuthenticated
        )
      }
    }
  }
}

@Composable
private fun AddressSection(
  aryaSamaj: AryaSamajDetail,
  onOpenInMaps: (Double, Double) -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp)
  ) {
    Column(
      modifier = Modifier.padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "पता",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Medium
        )

        // Navigation button if location is available
        aryaSamaj.address.location?.let { location ->
          IconButton(
            onClick = { onOpenInMaps(location.latitude, location.longitude) }
          ) {
            Icon(
              Icons.Default.Navigation,
              contentDescription = "दिशा-निर्देश",
              tint = MaterialTheme.colorScheme.primary
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Full address
      Text(
        text =
          buildString {
            if (aryaSamaj.address.address.isNotBlank()) {
              append(aryaSamaj.address.address)
            }
            if (aryaSamaj.address.district.isNotBlank()) {
              if (isNotBlank()) append(", ")
              append(aryaSamaj.address.district)
            }
            if (aryaSamaj.address.state.isNotBlank()) {
              if (isNotBlank()) append(", ")
              append(aryaSamaj.address.state)
            }
            if (aryaSamaj.address.pincode.isNotBlank()) {
              if (isNotBlank()) append(" - ")
              append(aryaSamaj.address.pincode)
            }
          },
        style = MaterialTheme.typography.bodyMedium
      )

      // Vidhansabha if available
      if (aryaSamaj.address.vidhansabha.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "विधानसभा: ${aryaSamaj.address.vidhansabha}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun DescriptionSection(
  description: String,
  isEditable: Boolean
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp)
  ) {
    Column(
      modifier = Modifier.padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "विवरण",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Medium
        )

        if (isEditable) {
          // Temporarily hidden - Edit button
          /*
          IconButton(onClick = { /* TODO: Edit description */ }) {
            Icon(
              Icons.Default.Edit,
              contentDescription = "विवरण संपादित करें",
              modifier = Modifier.size(20.dp)
            )
          }
          */
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium
      )
    }
  }
}

@Composable
private fun PicturesSection(mediaUrls: List<String>) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp)
  ) {
    Column(
      modifier = Modifier.padding(16.dp)
    ) {
      Text(
        text = "चित्र",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium
      )

      Spacer(modifier = Modifier.height(12.dp))

      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(mediaUrls) { imageUrl ->
          AsyncImage(
            model = imageUrl,
            contentDescription = "आर्य समाज के चित्र",
            modifier =
              Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
          )
        }
      }
    }
  }
}

@Composable
private fun MembersSection(
  members: List<AryaSamajMember>,
  isEditable: Boolean
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp)
  ) {
    Column(
      modifier = Modifier.padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "कार्यकारिणी/पदाधिकारी",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Medium
        )

        if (isEditable) {
          // Temporarily hidden - Edit button
          /*
          IconButton(onClick = { /* TODO: Edit members */ }) {
            Icon(
              Icons.Default.Edit,
              contentDescription = "सदस्य संपादित करें",
              modifier = Modifier.size(20.dp),
              tint = MaterialTheme.colorScheme.primary
            )
          }
          */
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Sort members by priority
      val sortedMembers = members.sortedBy { it.priority }

      sortedMembers.forEach { member ->
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Profile image
          if (!member.memberProfileImage.isNullOrEmpty()) {
            AsyncImage(
              model = member.memberProfileImage,
              contentDescription = "Profile Image",
              modifier =
                Modifier
                  .size(40.dp)
                  .clip(RoundedCornerShape(20.dp)),
              contentScale = ContentScale.Crop
            )
          } else {
            Surface(
              modifier = Modifier.size(40.dp),
              shape = RoundedCornerShape(20.dp),
              color = MaterialTheme.colorScheme.surfaceVariant
            ) {
              Box(
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  Icons.Default.Person,
                  contentDescription = "Profile",
                  modifier = Modifier.size(20.dp),
                  tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = member.memberName,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium
            )
            if (member.post.isNotBlank()) {
              Text(
                text = member.post,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          if (!member.memberPhoneNumber.isNullOrEmpty()) {
            IconButton(
              onClick = { /* TODO: Call functionality */ }
            ) {
              Icon(
                Icons.Default.Phone,
                contentDescription = "कॉल करें",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
              )
            }
          }
        }

        if (member != sortedMembers.last()) {
          HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant
          )
        }
      }
    }
  }
}
