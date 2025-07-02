package com.aryamahasangh.features.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.aryamahasangh.fragment.FamilyFields
import com.aryamahasangh.navigation.LocalSnackbarHostState
import com.aryamahasangh.utils.WithTooltip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyDetailScreen(
  familyId: String,
  viewModel: FamilyViewModel,
  onNavigateBack: () -> Unit,
  onEditFamily: () -> Unit = {}
) {
  val snackbarHostState = LocalSnackbarHostState.current
  val uriHandler = LocalUriHandler.current
  val familyDetailUiState by viewModel.familyDetailUiState.collectAsState()

  LaunchedEffect(familyId) {
    viewModel.getFamilyDetail(familyId)
  }

  // Handle error state
  familyDetailUiState.error?.let { error ->
    LaunchedEffect(error) {
      snackbarHostState.showSnackbar(error)
      viewModel.clearError()
    }
  }

  // Handle loading state
  if (familyDetailUiState.isLoading) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      CircularProgressIndicator()
    }
    return
  }

  // Handle empty/error state
  val familyData = familyDetailUiState.familyDetail?.familyFields
  if (familyData == null) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text("परिवार की जानकारी लोड नहीं हो सकी")
        Button(onClick = {
          viewModel.getFamilyDetail(familyId)
        }) {
          Text("पुनः प्रयास करें")
        }
      }
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
    // Header with family name and edit button
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = familyData.name ?: "अज्ञात परिवार",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )

        WithTooltip(tooltip = "परिवार की जानकारी संपादित करें") {
          IconButton(onClick = onEditFamily) {
            Icon(
              Icons.Default.Edit,
              contentDescription = "संपादित करें",
              tint = MaterialTheme.colorScheme.primary
            )
          }
        }
      }
    }

    // Family photos section
    if (familyData.photos?.isNotEmpty() == true) {
      item {
        FamilyPhotosSection(photos = familyData.photos.filterNotNull())
      }
    }

    // Family members section
    item {
      FamilyMembersSection(members = familyData.familyMemberCollection?.edges ?: emptyList())
    }

    // Address section
    familyData.address?.let { address ->
      item {
        AddressSection(
          address = address,
          onNavigateToAddress = { latitude, longitude ->
            val uri = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
            uriHandler.openUri(uri)
          }
        )
      }
    }

    // Associated Arya Samaj section
    familyData.aryaSamaj?.let { aryaSamaj ->
      item {
        AryaSamajSection(aryaSamaj = aryaSamaj)
      }
    }
  }
}

@Composable
private fun FamilyPhotosSection(photos: List<String>) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
      )
  ) {
    Column(
      modifier = Modifier.padding(20.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          Icons.Default.PhotoLibrary,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
          text = "पारिवारिक चित्र",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.SemiBold
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(photos) { photoUrl ->
          AsyncImage(
            model = photoUrl,
            contentDescription = "पारिवारिक चित्र",
            modifier =
              Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
          )
        }
      }
    }
  }
}

@Composable
private fun FamilyMembersSection(members: List<FamilyFields.Edge>) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
      )
  ) {
    Column(
      modifier = Modifier.padding(20.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          Icons.Default.Group,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
          text = "परिवार सदस्य",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.SemiBold
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Sort members: head first, then by creation date
      val sortedMembers =
        members.sortedWith(
          compareByDescending<FamilyFields.Edge> { it.node.isHead }
            .thenBy { it.node.createdAt }
        )

      sortedMembers.forEachIndexed { index, memberEdge ->
        val member = memberEdge.node

        FamilyMemberCard(
          member = member,
          isHead = member.isHead
        )

        if (index < sortedMembers.size - 1) {
          Spacer(modifier = Modifier.height(12.dp))
        }
      }
    }
  }
}

@Composable
private fun FamilyMemberCard(
  member: FamilyFields.Node,
  isHead: Boolean
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(4.dp),
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (isHead) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
          } else {
            MaterialTheme.colorScheme.surface
          }
      ),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Profile image
      val memberDetails = member.member?.memberDetails
      if (!memberDetails?.profileImage.isNullOrEmpty()) {
        AsyncImage(
          model = memberDetails?.profileImage,
          contentDescription = "प्रोफ़ाइल चित्र",
          modifier =
            Modifier
              .size(56.dp)
              .clip(RoundedCornerShape(28.dp)),
          contentScale = ContentScale.Crop
        )
      } else {
        Surface(
          modifier = Modifier.size(56.dp),
          shape = RoundedCornerShape(28.dp),
          color = MaterialTheme.colorScheme.surfaceVariant
        ) {
          Box(
            contentAlignment = Alignment.Center
          ) {
            Icon(
              Icons.Default.Person,
              contentDescription = "प्रोफ़ाइल",
              modifier = Modifier.size(32.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(16.dp))

      Column(modifier = Modifier.weight(1f)) {
        // Name with head indicator
        Row(
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = memberDetails?.name ?: "अज्ञात",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
          )

          if (isHead) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.primary
            ) {
              Text(
                text = "प्रमुख",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }
        }

        // Relation to head
        if (!isHead) {
          val relation =
            try {
              member.relationToHead.toComponents().toDisplayName()
            } catch (e: Exception) {
              "अज्ञात संबंध"
            }

          Text(
            text = "संबंध: $relation",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
          )
        }

        // Joining date if available
        memberDetails?.joiningDate?.let { joiningDate ->
          Text(
            text = "संगठन के साथ ${joiningDate.toHindiDateString()} से",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
          )
        }

        // Phone number if available
        if (!memberDetails?.phoneNumber.isNullOrEmpty()) {
          Text(
            text = "दूरभाष: ${memberDetails?.phoneNumber}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun AddressSection(
  address: FamilyFields.Address,
  onNavigateToAddress: (Double, Double) -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
      )
  ) {
    Column(
      modifier = Modifier.padding(20.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = "पता",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
          )
        }

        // Navigation button if coordinates are available
        if (address.latitude != null && address.longitude != null) {
          WithTooltip(tooltip = "दिशा-निर्देश प्राप्त करें") {
            IconButton(
              onClick = {
                onNavigateToAddress(address.latitude, address.longitude)
              }
            ) {
              Icon(
                Icons.Default.Navigation,
                contentDescription = "दिशा-निर्देश",
                tint = MaterialTheme.colorScheme.primary
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Full address
      Text(
        text =
          buildString {
            if (!address.basicAddress.isNullOrBlank()) {
              append(address.basicAddress)
            }
            if (!address.district.isNullOrBlank()) {
              if (isNotBlank()) append(", ")
              append(address.district)
            }
            if (!address.state.isNullOrBlank()) {
              if (isNotBlank()) append(", ")
              append(address.state)
            }
            if (!address.pincode.isNullOrBlank()) {
              if (isNotBlank()) append(" - ")
              append(address.pincode)
            }
          },
        style = MaterialTheme.typography.bodyLarge,
        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight.times(1.2f)
      )

      // Vidhansabha if available
      if (!address.vidhansabha.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            Icons.Default.AccountBalance,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "विधानसभा: ${address.vidhansabha}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@Composable
private fun AryaSamajSection(aryaSamaj: FamilyFields.AryaSamaj) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
      )
  ) {
    Column(
      modifier = Modifier.padding(20.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          Icons.Default.Home,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
          text = "संबंधित आर्य समाज",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.SemiBold
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = aryaSamaj.name ?: "अज्ञात आर्य समाज",
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium
      )

      // Arya Samaj address if available
      aryaSamaj.address?.let { address ->
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text =
            buildString {
              if (!address.basicAddress.isNullOrBlank()) {
                append(address.basicAddress)
              }
              if (!address.district.isNullOrBlank()) {
                if (isNotBlank()) append(", ")
                append(address.district)
              }
              if (!address.state.isNullOrBlank()) {
                if (isNotBlank()) append(", ")
                append(address.state)
              }
            },
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}
