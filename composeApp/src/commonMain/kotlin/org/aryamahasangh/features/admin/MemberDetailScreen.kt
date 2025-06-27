package org.aryamahasangh.features.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.datetime.LocalDate
import org.aryamahasangh.components.Gender
import org.aryamahasangh.features.arya_nirman.convertDates
import org.aryamahasangh.fragment.AddressFields
import org.aryamahasangh.fragment.AryaSamajFields
import org.aryamahasangh.navigation.LocalSnackbarHostState
import org.aryamahasangh.utils.WithTooltip

// Extension function to format address
fun AddressFields.formatAddress(): String {
  return buildString {
    if (!basicAddress.isNullOrEmpty()) append(basicAddress)
    if (!district.isNullOrEmpty()) {
      if (isNotEmpty()) append(", ")
      append(district)
    }
    if (!state.isNullOrEmpty()) {
      if (isNotEmpty()) append(", ")
      append(state)
    }
    if (!pincode.isNullOrEmpty()) {
      if (isNotEmpty()) append(" - ")
      append(pincode)
    }
  }
}

// Extension function to format AryaSamaj address
fun AryaSamajFields.formatAddress(): String {
  return address?.addressFields?.formatAddress() ?: ""
}

// Extension function to format dates in a user-friendly way
fun LocalDate.toDisplayString(): String {
  return "${this.dayOfMonth}/${this.monthNumber}/${this.year}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDetailScreen(
  memberId: String,
  viewModel: AdminViewModel,
  onNavigateBack: () -> Unit = {},
  onNavigateToEdit: (String) -> Unit = {} // Add navigation to edit
) {
  val uiState by viewModel.memberDetailUiState.collectAsState()
  val snackbarHostState = LocalSnackbarHostState.current

  LaunchedEffect(memberId) {
    viewModel.loadMemberDetail(memberId)
  }

  if (uiState.isLoading) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      CircularProgressIndicator()
    }
    return
  }

  Column(modifier = Modifier.fillMaxSize()) {
    // Top app bar with edit button
    TopAppBar(
      title = { Text("सदस्य विवरण") },
      actions = {
        IconButton(onClick = { onNavigateToEdit(memberId) }) {
          Icon(Icons.Filled.Edit, contentDescription = "सदस्य संपादित करें")
        }
      }
    )

    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item {
        // Profile Section
        ProfileSection(
          member = uiState.member
        )
      }

      item {
        // Details Section
        DetailsSection(
          member = uiState.member
        )
      }

      item {
        // Address Section
        if (uiState.member != null) {
          AddressSection(
            address = uiState.member!!.addressFields
          )
        }
      }

      // Show referrer, organisations and activities
      if (uiState.member != null) {
        // Referrer Section
        uiState.member!!.referrer?.let { referrer ->
          item {
            ReferrerSection(referrer = referrer)
          }
        }

        // Arya Samaj Section
        uiState.member!!.aryaSamaj?.let { aryaSamaj ->
          item {
            AryaSamajSection(aryaSamaj = aryaSamaj)
          }
        }

        item {
          OrganisationsSection(organisations = uiState.member!!.organisations)
        }

        item {
          ActivitiesSection(activities = uiState.member!!.activities)
        }

        // Samaj Positions Section - Show Arya Samaj positions
        if (uiState.member!!.samajPositions.isNotEmpty()) {
          item {
            SamajPositionsSection(samajPositions = uiState.member!!.samajPositions)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileSection(
  member: MemberDetail?
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(4.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Profile Image
      AsyncImage(
        model =
          member?.profileImage?.ifEmpty { "https://via.placeholder.com/100" }
            ?: "https://via.placeholder.com/100",
        contentDescription = "Profile Image",
        modifier =
          Modifier
            .size(100.dp)
            .clip(CircleShape)
      )

      Spacer(modifier = Modifier.width(16.dp))

      // Member Info
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = member?.name ?: "Unknown",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold
        )
        if (member != null) {
          val address =
            buildString {
              if (member.address.isNotEmpty()) append(member.address)
              if (member.district.isNotEmpty()) {
                if (isNotEmpty()) append(", ")
                append(member.district)
              }
              if (member.state.isNotEmpty()) {
                if (isNotEmpty()) append(", ")
                append(member.state)
              }
              if (member.pincode.isNotEmpty()) {
                if (isNotEmpty()) append(" - ")
                append(member.pincode)
              }
            }
          if (address.isNotEmpty()) {
            Text(
              text = address,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.secondary
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsSection(
  member: MemberDetail?
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(4.dp)
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "विवरण",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      member?.let { memberData ->
        DetailItem("नाम", memberData.name)
        DetailItem("दूरभाष", memberData.phoneNumber)
        DetailItem("ईमेल", memberData.email)
        DetailItem("शैक्षणिक योग्यता", memberData.educationalQualification)

        // New fields
        memberData.dob?.let { dob ->
          DetailItem("जन्मतिथि", dob.toDisplayString())
        }

        memberData.joiningDate?.let { joinDate ->
          DetailItem("संगठन से जुड़ने की तिथि", joinDate.toDisplayString())
        }

        memberData.gender?.let { gender ->
          val genderText = when (gender) {
            Gender.MALE -> "पुरुष"
            Gender.FEMALE -> "स्त्री"
            Gender.OTHER -> "अन्य"
          }
          DetailItem("लिंग", genderText)
        }

        DetailItem("व्यवसाय", memberData.occupation)
        DetailItem("परिचय", memberData.introduction)
      }
    }
  }
}

@Composable
private fun AddressSection(
  address: AddressFields?
) {
  val uriHandler = LocalUriHandler.current

  if (address != null) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
      )
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp)
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
            WithTooltip(tooltip = "मानचित्र पर देखें") {
              IconButton(
                onClick = {
                  val uri = "https://www.google.com/maps/search/?api=1&query=${address.latitude},${address.longitude}"
                  uriHandler.openUri(uri)
                }
              ) {
                Icon(
                  Icons.Default.Navigation,
                  contentDescription = "मानचित्र पर देखें",
                  tint = MaterialTheme.colorScheme.primary
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Full address
        Text(
          text = address.formatAddress(),
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
              Icons.Default.LocationOn,
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
}

@Composable
private fun DetailItem(
  label: String,
  value: String
) {
  if (value.isNotEmpty()) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.secondary
      )
      Text(
        text = value,
        style = MaterialTheme.typography.bodyLarge
      )
    }
  }
}

@Composable
private fun ReferrerSection(referrer: ReferrerInfo) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(4.dp)
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
      Text(
        text = "संदर्भक (Referrer)",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )
      Spacer(modifier = Modifier.height(4.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Profile Image
        AsyncImage(
          model = referrer.profileImage.ifEmpty { "https://via.placeholder.com/60" },
          contentDescription = "Referrer Profile Image",
          modifier = Modifier
            .size(60.dp)
            .clip(CircleShape),
          contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Referrer Info
        Column(
          modifier = Modifier.weight(1f)
        ) {
          Text(
            text = referrer.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AryaSamajSection(aryaSamaj: AryaSamajFields) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainer
    )
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(20.dp)
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
          text = address.addressFields?.formatAddress() ?: "",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun OrganisationsSection(organisations: List<OrganisationInfo>) {
  if (organisations.isNotEmpty()) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(4.dp)
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp)
      ) {
        Text(
          text = "संबधित संस्थाएं",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        organisations.forEach { org ->
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            AsyncImage(
              model = org.logo.ifEmpty { "https://via.placeholder.com/40" },
              contentDescription = "Organisation Logo",
              modifier = Modifier.size(40.dp).clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = org.name, style = MaterialTheme.typography.bodyLarge)
          }
        }
      }
    }
  }
}

@Composable
private fun ActivitiesSection(activities: List<ActivityInfo>) {
  if (activities.isNotEmpty()) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(4.dp)
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp)
      ) {
        Text(
          text = "संबधित गतिविधियाँ",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        activities.forEach { activity ->
          Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            shape = RoundedCornerShape(4.dp)
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(12.dp)
            ) {
              Text(
                text = activity.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
              )
              Text(
                text =
                  buildAnnotatedString {
                    val subtleTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    val (dateRange, timeRange) = convertDates(activity.startDatetime, activity.endDatetime)
                    withStyle(
                      style =
                        SpanStyle(
                          fontWeight = FontWeight.SemiBold,
                          fontSize = 16.sp,
                          color = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                      append(dateRange)
                    }
                    append(" | ")
                    withStyle(style = SpanStyle(fontSize = 13.sp, color = subtleTextColor)) {
                      append(timeRange)
                    }
                  },
                style = MaterialTheme.typography.bodyMedium
              )
              Text(
                text = "${activity.district}, ${activity.state}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SamajPositionsSection(samajPositions: List<SamajPositionInfo>) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(4.dp)
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
      Text(
        text = "आर्य समाज में पद",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(8.dp))

      samajPositions.forEach { position ->
        Card(
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
          shape = RoundedCornerShape(4.dp)
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp)
          ) {
            Text(
              text = position.aryaSamaj.name,
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "पद: ${position.post}",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }
}
