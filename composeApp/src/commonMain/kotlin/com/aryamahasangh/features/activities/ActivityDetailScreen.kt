package com.aryamahasangh.features.activities

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import aryamahasangh.composeapp.generated.resources.Res
import aryamahasangh.composeapp.generated.resources.error_profile_image
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import com.aryamahasangh.LocalIsAuthenticated
import com.aryamahasangh.components.activityTypeData
import com.aryamahasangh.isWeb
import com.aryamahasangh.navigation.LocalSnackbarHostState
import com.aryamahasangh.utils.format
import com.aryamahasangh.utils.openDirections
import com.aryamahasangh.utils.toHumanReadable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
fun ActivityDetailScreen(
  id: String,
  onNavigateToEdit: (String) -> Unit = {},
  onNavigateToRegistration: (String, Int) -> Unit = { _, _ -> },
  onNavigateToCreateOverview: (String, String?, List<String>) -> Unit = { _, _, _ -> },
  viewModel: ActivitiesViewModel = koinInject()
) {
  val snackbarHostState = LocalSnackbarHostState.current

  val isLoggedIn = LocalIsAuthenticated.current

  // Load activity details
  LaunchedEffect(id, isLoggedIn) {
    println("Loading activity details for ID: $id")
    viewModel.loadActivityDetail(id)
    // Don't call loadRegisteredUsers here - it's already handled in loadActivityDetail with polling
  }

  // Stop listening when navigating away
  DisposableEffect(id) {
    println("DisposableEffect created for activity: $id")
    onDispose {
      println("DisposableEffect disposing for activity: $id")
      viewModel.stopListeningForRegistrations()
    }
  }

  // Collect UI state from ViewModel
  val uiState by viewModel.activityDetailUiState.collectAsState()
  val registeredUsers by viewModel.registeredUsers.collectAsState()

  // Handle loading state
  if (uiState.isLoading) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      LinearProgressIndicator()
    }
    return
  }

  // Handle error state
  uiState.error?.let { error ->
    LaunchedEffect(error) {
      snackbarHostState.showSnackbar(
        message = error,
        actionLabel = "Retry"
      )
    }

    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text("Failed to load activity details")
        Button(onClick = { viewModel.loadActivityDetail(id) }) {
          Text("Retry")
        }
      }
    }
    return
  }

  // Handle null activity
  if (uiState.activity == null) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Text("Activity not found")
    }
    return
  }

  Box(modifier = Modifier.fillMaxSize()) {
    ActivityDisplay(
      activity = uiState.activity!!,
      registeredUsers = registeredUsers,
      isLoggedIn = isLoggedIn,
      onNavigateToRegistration = onNavigateToRegistration,
      onNavigateToCreateOverview = onNavigateToCreateOverview
    )

    // Edit button in top-right corner
    if (isLoggedIn) {
      if (uiState.activity!!.isUpcoming()) {
        IconButton(
          onClick = { onNavigateToEdit(id) },
          modifier =
            Modifier
              .align(Alignment.TopEnd)
              .padding(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "गतिविधि संपादित करें",
            tint = MaterialTheme.colorScheme.primary
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class) // For ListItem
@Composable
fun UserProfileListItem(user: UserProfile, modifier: Modifier = Modifier) {
  // Using Material 3 ListItem for standard list item appearance and structure
  val uriHandler = LocalUriHandler.current
  val snackbarHostState = LocalSnackbarHostState.current
  val scope = rememberCoroutineScope()
  ListItem(
    modifier = modifier.widthIn(max = 500.dp),
    headlineContent = {
      Text(
        text = user.fullname,
        style = MaterialTheme.typography.titleMedium,
        maxLines = 1, // Ensure full name doesn't wrap excessively if very long
        overflow = TextOverflow.Ellipsis
      )
    },
    supportingContent = {
      Text(
        text = user.address,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    },
    leadingContent = {
      val genderIcon: ImageVector =
        if (user.gender.equals("female", ignoreCase = true)) {
          Icons.Filled.Face4
        } else {
          Icons.Filled.Person
        }
      // Circular background for the icon
      Box(
        modifier =
          Modifier
            .size(48.dp) // Standard M3 size for leading icon container
            .clip(CircleShape) // Apply circular clipping
            .background(MaterialTheme.colorScheme.primaryContainer),
        // Background color for the circle
        contentAlignment = Alignment.Center // Center the icon within the Box
      ) {
        Icon(
          imageVector = genderIcon,
          contentDescription = user.gender,
          tint = MaterialTheme.colorScheme.onPrimaryContainer, // Icon color on top of primaryContainer
          modifier = Modifier.size(36.dp) // Standard M3 icon size
        )
      }
    },
    trailingContent = {
      IconButton(
        onClick = {
          uriHandler.openUri("tel:${user.mobile}")
          if (isWeb()) {
            scope.launch {
              snackbarHostState.showSnackbar(
                message = "If you do not have calling apps installed, you can manually call to ${user.mobile}",
                actionLabel = "Close"
              )
            }
          }
        }
        // Modifier.align(Alignment.CenterVertically) is handled by ListItem for trailingContent
      ) {
        Icon(
          imageVector = Icons.Filled.Call,
          contentDescription = "Call ${user.fullname}",
          tint = MaterialTheme.colorScheme.primary
        )
      }
    },
    colors =
      ListItemDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) // Optional: slight background
      ),
    tonalElevation = 1.dp // Optional: adds a slight shadow
  )
}

@Composable
fun RegisteredUsers(
  users: List<UserProfile>,
  capacity: Int = 0
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(8.dp) // Space between items
  ) {
    Column {
      val count = "${users.size}"
      val capacityText = if (capacity > 0) " / $capacity" else ""
      Text(
        "कुल पंजीकरण (${count.toDevanagariNumerals()}${capacityText.toDevanagariNumerals()}):",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 16.dp)
      )
    }
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      users.forEach { user ->
        UserProfileListItem(user = user)
      }
    }
  }
}

// @Preview
// @Composable
// fun ActivityDisplayPreview(){
//  val activity = OrganisationalActivityDetailQuery.OrganisationalActivity(
//    id = "",
//    name =  "नियमित संध्या अनुषठान अभियान",
//    description = "ईश के ज्ञान से लोक में जांच के आर्य कार्य आगे बढ़ाते रहें। नित्य है ना मिटे ना हटे ले चले प्रार्थना प्रेम से भाव लाते रहें।",
//        associatedOrganisations = listOf(
//          OrganisationalActivityDetailQuery.AssociatedOrganisation("sdfsdfdsf", "राष्ट्रीय आर्य निर्मात्री सभा")
//        ),
//        activityType = ActivityType.EVENT,
//        district =  "रोहतक",
//        startDateTime = "2025-02-25T09:04:42.006965",
//        endDateTime = "2025-05-15T09:04:42.006965",
//        mediaFiles = listOf(
//        "https://images.pexels.com/photos/209831/pexels-photo-209831.jpeg?auto=compress&cs=tinysrgb&w=200&dpr=1&fit=crop&h=150",
//        "https://images.pexels.com/photos/1402787/pexels-photo-1402787.jpeg?auto=compress&cs=tinysrgb&w=200&dpr=1&fit=crop&h=150"
//        ),
//        additionalInstructions = "शान्तिपाठ + जयघोष अभिवादन + प्रसाद वितरण।\nसभी आर्यसमाज पदाधिकारी अवश्य पहुंचें और संगठित स्वरूप को प्रकाशित करें !!",
//      contactPeople = listOf(
//        ContactPeople(
//          member = OrganisationalActivityDetailQuery.Member(
//            name = "आचार्य संजीव आर्य",
//            profileImage = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/achary_sanjiv.webp",
//            phoneNumber = "9045353309"
//          ),
//          post = "अध्यक्ष",
//          priority = 1
//        )
//      ),
//        address = "sdfsdf",
//        state = "sdffsdf"
//  )
//  Surface(Modifier.background(Color.White)){
//    ActivityDisplay(activity)
//  }
// }

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ActivityDisplay(
  activity: OrganisationalActivity,
  registeredUsers: List<UserProfile> = emptyList(),
  isLoggedIn: Boolean,
  onNavigateToRegistration: (String, Int) -> Unit = { _, _ -> },
  onNavigateToCreateOverview: (String, String?, List<String>) -> Unit = { _, _, _ -> }
) {
  println(activity)

  // Profile Image URLS
  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .padding(8.dp)
        .verticalScroll(rememberScrollState())
  ) {
    // Name and Activity Type
    Column {
      Text(
        text = activity.name,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        modifier =
          Modifier.background(
            MaterialTheme.colorScheme.outlineVariant
          ).padding(vertical = 4.dp, horizontal = 16.dp),
        text = "${activityTypeData[activity.type]}",
        style = MaterialTheme.typography.bodyLarge
      )
    }

    // Description
    Text(
      text = activity.shortDescription,
      style = MaterialTheme.typography.bodyLarge,
      modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
    Text(
      text = activity.longDescription,
      style = MaterialTheme.typography.bodyLarge,
      modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
    )
    Spacer(modifier = Modifier.height(4.dp))

    // Associated Organisations
    Text(
      text = "संबंधित संस्थाएँ:",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold
    )
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalArrangement = Arrangement.spacedBy(-12.dp)
    ) {
      activity.associatedOrganisations.forEach { associatedOrg ->
        AssistChip(
          onClick = { },
          label = { Text(associatedOrg.organisation.name) }
        )
      }
    }

    // Place - only show if address data is present
    val hasAddressData =
      activity.address.isNotEmpty() || activity.state.isNotEmpty() ||
        activity.district.isNotEmpty() || activity.latitude != null || activity.longitude != null

    if (hasAddressData) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(imageVector = Icons.Default.LocationOn, contentDescription = "Place", tint = Color.Gray)
        Spacer(modifier = Modifier.width(4.dp))

        val addressParts =
          listOfNotNull(
            activity.address.takeIf { it.isNotEmpty() },
            activity.district.takeIf { it.isNotEmpty() },
            activity.state.takeIf { it.isNotEmpty() }
          )

        Text(
          text = "स्थान: ${addressParts.joinToString(", ")}${if (addressParts.isNotEmpty()) "." else ""}",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.weight(1f)
        )

        if (activity.latitude != null && activity.longitude != null) {
          val uriHandler = LocalUriHandler.current
          IconButton(
            onClick = {
              openDirections(
                uriHandler,
                activity.latitude!!,
                activity.longitude!!,
                "${activity.address}, ${activity.district}"
              )
            }
          ) {
            Icon(
              imageVector = Icons.Default.Navigation,
              contentDescription = "दिशा-निर्देश",
              tint = MaterialTheme.colorScheme.primary
            )
          }
        }
      }
    }

    // Start and End Date/Time
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(top = 4.dp)
    ) {
      Icon(imageVector = Icons.Default.DateRange, contentDescription = "Start Date", tint = Color.Gray)
      Spacer(modifier = Modifier.width(4.dp))
      Text(text = "प्रारंभ: ${activity.startDatetime.toHumanReadable()}", style = MaterialTheme.typography.bodyMedium)
    }

    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(bottom = 8.dp)
    ) {
      Icon(imageVector = Icons.Default.DateRange, contentDescription = "End Date", tint = Color.Gray)
      Spacer(modifier = Modifier.width(4.dp))
      Text(text = "समाप्ति: ${activity.endDatetime.toHumanReadable()}", style = MaterialTheme.typography.bodyMedium)
    }

    val uriHandler = LocalUriHandler.current

    // Media Files
    if (activity.mediaFiles.isNotEmpty()) {
      LazyRow(
        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(activity.mediaFiles) { imageUrl ->
          AsyncImage(
            model = imageUrl,
            contentDescription = "Thumbnail ",
            contentScale = ContentScale.Crop,
            modifier =
              Modifier.size(150.dp).clickable(onClick = {
                uriHandler.openUri(imageUrl)
              })
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(4.dp))
    // Contact People
    Text(
      text = "संपर्क सूत्र:",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )

    val sortedContactPeople = activity.contactPeople.sortedBy { it.priority }
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      sortedContactPeople.forEach { contactPerson ->
        ContactPersonItem(contactPerson = contactPerson)
      }
    }

    // Additional Instructions
    if (activity.additionalInstructions.isNotEmpty()) {
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = "अतिरिक्त निर्देश:",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
      )

      val instructions = activity.additionalInstructions.split("\n")
      Column {
        instructions.forEach { instruction ->
          Row(verticalAlignment = Alignment.Top) {
            Text(text = "• ", modifier = Modifier.padding(end = 4.dp))
            Text(text = instruction, style = MaterialTheme.typography.bodyMedium)
          }
        }
      }
    }

    // Registration Button for upcoming activities
    if (activity.isUpcoming()) {
      Spacer(modifier = Modifier.height(16.dp))
      val registrationCount = registeredUsers.size
      val hasCapacityLimit = activity.capacity > 0
      val isFull = hasCapacityLimit && registrationCount >= activity.capacity

      println(
        "Activity ${activity.id}: registrations=$registrationCount, capacity=${activity.capacity}, isFull=$isFull"
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = { onNavigateToRegistration(activity.id, activity.capacity) },
          enabled = !isFull
        ) {
          Text(
            text = if (isFull) "पंजीकरण बंद" else "पंजीकरण",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 24.dp)
          )
        }

        // Show overview write button next to registration if applicable
        if (isLoggedIn && !activity.hasOverview() && (activity.isFromPast() || activity.getStatus() == ActivityStatus.ONGOING)) {
          Button(
            onClick = {
              onNavigateToCreateOverview(
                activity.id,
                activity.overviewDescription,
                activity.overviewMediaUrls
              )
            },
            modifier = Modifier.weight(1f)
          ) {
            Text(
              text = "अवलोकन लिखें",
              style = MaterialTheme.typography.labelLarge
            )
          }
        }
      }
    }

    // Overview and Registration sections with tabs for logged in users
    if (activity.hasOverview() && isLoggedIn) {
      Spacer(modifier = Modifier.height(16.dp))

      if (isLoggedIn) {
        // Tabs for logged in users
        var selectedTabIndex by remember { mutableStateOf(0) }
        val tabs =
          listOf(
            "अवलोकन",
            "कुल पंजीकरण (${
              registeredUsers.size.toString().toDevanagariNumerals()
            }${if (activity.capacity > 0) " / ${activity.capacity.toString().toDevanagariNumerals()}" else ""})"
          )

        PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
          tabs.forEachIndexed { index, title ->
            Tab(
              selected = selectedTabIndex == index,
              onClick = { selectedTabIndex = index },
              text = { Text(title) }
            )
          }
        }

        when (selectedTabIndex) {
          0 -> {
            if (activity.hasOverview()) {
              OverviewSection(
                activity = activity,
                isLoggedIn = isLoggedIn,
                onNavigateToCreateOverview = onNavigateToCreateOverview
              )
            } else {
              Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp)
              ) {
                Column(
                  modifier = Modifier.padding(16.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Text(
                    "अभी तक कोई अवलोकन नहीं जोड़ा गया है",
                    style = MaterialTheme.typography.bodyLarge
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  Button(
                    onClick = {
                      onNavigateToCreateOverview(
                        activity.id,
                        activity.overviewDescription,
                        activity.overviewMediaUrls
                      )
                    }
                  ) {
                    Text("अवलोकन लिखें")
                  }
                }
              }
            }
          }

          1 -> {
            RegisteredUsers(registeredUsers, activity.capacity)
          }
        }
      } else {
        // Only overview for non-logged in users
        if (activity.hasOverview()) {
          OverviewSection(
            activity = activity,
            isLoggedIn = isLoggedIn,
            onNavigateToCreateOverview = onNavigateToCreateOverview
          )
        }
      }
    } else if( activity.hasOverview() ){
      OverviewSection(
        activity = activity,
        isLoggedIn = isLoggedIn,
        onNavigateToCreateOverview = onNavigateToCreateOverview
      )
    } else if (isLoggedIn) {
      RegisteredUsers(registeredUsers, activity.capacity)
    }
  }
}

@Composable
fun OverviewSection(
  activity: OrganisationalActivity,
  isLoggedIn: Boolean,
  onNavigateToCreateOverview: (String, String?, List<String>) -> Unit
) {
  Column(
    modifier = Modifier.padding(8.dp)
  ) {
    if (!isLoggedIn) {
      Text(
        text = "अवलोकन",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
      HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.End,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Edit button for logged in users
      if (isLoggedIn && activity.hasOverview()) {
        IconButton(
          onClick = {
            onNavigateToCreateOverview(
              activity.id,
              activity.overviewDescription,
              activity.overviewMediaUrls
            )
          }
        ) {
          Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "अवलोकन संपादित करें",
            tint = MaterialTheme.colorScheme.primary
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (activity.overviewMediaUrls.isNotEmpty()) {
      Spacer(modifier = Modifier.height(12.dp))
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        val uriHandler = LocalUriHandler.current
        activity.overviewMediaUrls.forEach { imageUrl ->
          AsyncImage(
            model = imageUrl,
            contentDescription = "अवलोकन चित्र",
            contentScale = ContentScale.Fit,
            modifier =
              Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                  // Handle image click if needed - could open in full screen
                  uriHandler.openUri(imageUrl)
                }
          )
        }
      }
    }
    Spacer(modifier = Modifier.height(16.dp))
    if (!activity.overviewDescription.isNullOrEmpty()) {
      Text(
        text = activity.overviewDescription,
        style = MaterialTheme.typography.bodyLarge
      )
    }

    if (isLoggedIn && !activity.hasOverview()) {
      Button(
        onClick = {
          onNavigateToCreateOverview(
            activity.id,
            activity.overviewDescription,
            activity.overviewMediaUrls
          )
        },
        modifier = Modifier.padding(top = 16.dp)
      ) {
        Text(
          text = "अवलोकन लिखें",
          style = MaterialTheme.typography.labelLarge,
          modifier = Modifier.padding(horizontal = 24.dp)
        )
      }
    }
  }
}

@Preview
@Composable
fun ContactPersonPreview() {
  val contact =
    ActivityMember(
      id = "",
      member =
        Member(
          id = "",
          name = "आचार्य संजीव आर्य",
          profileImage = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/profile_image/achary_sanjiv.webp",
          phoneNumber = "9045353309"
        ),
      post = "अध्यक्ष",
      priority = 1
    )
  ContactPersonItem(contact)
}

@Composable
fun ContactPersonItem(contactPerson: ActivityMember) {
  Row(
    modifier = Modifier.heightIn(48.dp, 65.dp).padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Profile Image
    AsyncImage(
      model = contactPerson.member.profileImage,
      contentDescription = "Profile Image",
      contentScale = ContentScale.Crop,
      modifier =
        Modifier
          .size(50.dp)
          .clip(CircleShape),
      placeholder =
        BrushPainter(
          Brush.linearGradient(
            listOf(
              Color(color = 0xFFFFFFFF),
              Color(color = 0xFFDDDDDD)
            )
          )
        ),
      fallback = painterResource(Res.drawable.error_profile_image),
      error = painterResource(Res.drawable.error_profile_image)
    )

    Spacer(modifier = Modifier.width(8.dp))

    Column {
      Text(
        text = contactPerson.member.name,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = contactPerson.post,
        style = MaterialTheme.typography.bodySmall
      )
    }
    VerticalDivider(
      modifier = Modifier.padding(top = 12.dp, bottom = 12.dp, start = 12.dp),
      thickness = 2.dp,
      color = Color.LightGray
    )
    val uriHandler = LocalUriHandler.current
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    IconButton(
      onClick = {
        uriHandler.openUri("tel:${contactPerson.member.phoneNumber}")
        if (isWeb()) {
          scope.launch {
            snackbarHostState.showSnackbar(
              message = "If you do not have calling apps installed, you can manually call to ${contactPerson.member.phoneNumber}",
              actionLabel = "Close"
            )
          }
        }
      }
    ) {
      Icon(
        imageVector = Icons.Default.Call,
        contentDescription = "Call",
        tint = Color.Gray
      )
    }
  }
}

// Helper Functions

fun formatDateTime(dateTimeString: Any): String {
  return format(dateTimeString)
}
