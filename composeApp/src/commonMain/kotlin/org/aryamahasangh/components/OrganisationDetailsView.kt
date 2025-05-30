package org.aryamahasangh.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import aryamahasangh.composeapp.generated.resources.Res
import aryamahasangh.composeapp.generated.resources.baseline_groups
import aryamahasangh.composeapp.generated.resources.error_profile_image
import aryamahasangh.composeapp.generated.resources.mahasangh_logo_without_background
import coil3.compose.AsyncImage
import dev.burnoo.compose.remembersetting.rememberBooleanSetting
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.aryamahasangh.LocalSnackbarHostState
import org.aryamahasangh.SettingKeys
import org.aryamahasangh.features.organisations.*
import org.aryamahasangh.network.bucket
import org.aryamahasangh.screens.EditImageButton
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

fun drawableFromImageName(imageName: String) =
  when (imageName) {
//  "sanchar_parishad" -> Res.drawable.sanchar_parishad
    else -> Res.drawable.mahasangh_logo_without_background
  }

@Composable
@Preview
fun SabhaPreview() {
  Column(
    modifier =
      Modifier
        .verticalScroll(rememberScrollState())
  ) {
    // OrganisationDetail(listOfOrganisations[11])
  }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OrganisationDetail(
  organisation: OrganisationDetail,
  updateOrganisationLogo: (String, String, String) -> Unit,
  updateOrganisationDescription: (String, String) -> Unit,
  organisationDescriptionState: OrganisationDescriptionState = OrganisationDescriptionState(),
  onDescriptionEditModeChange: (Boolean) -> Unit = {},
  organisationLogoState: OrganisationLogoState = OrganisationLogoState(),
  onRemoveMember: (String, String, String) -> Unit = { _, _, _ -> },
  onUpdateMemberPost: (String, String, String) -> Unit = { _, _, _ -> },
  memberManagementState: MemberManagementState = MemberManagementState()
) {
  val (id, name, description, logo, keyPeople) = organisation
  var isLoggedIn by rememberBooleanSetting(SettingKeys.isLoggedIn, false)
  var showDeleteDialog by remember { mutableStateOf<OrganisationalMember?>(null) }
  var showUpdatePostDialog by remember { mutableStateOf<OrganisationalMember?>(null) }
  var updatedPost by remember { mutableStateOf("") }

  Column(
    modifier =
      Modifier.fillMaxSize().padding(8.dp)
        .verticalScroll(rememberScrollState())
  ) {
    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
      ) {
        if (isLoggedIn) {
          Row(modifier = Modifier.padding()) {
            val scope = rememberCoroutineScope()
            val snackbarHostState = LocalSnackbarHostState.current
            val launcher =
              rememberFilePickerLauncher(
                type = PickerType.Image,
                mode = PickerMode.Single,
                title = "Select logo"
              ) { file ->
                if (file != null) {
                  scope.launch {
                    try {
                      // Show immediate upload feedback
                      val snackbarJob = launch {
                        snackbarHostState.showSnackbar(
                          message = "🔄 Uploading new logo...",
                          duration = SnackbarDuration.Indefinite
                        )
                      }

                      val uploadResponse =
                        bucket.upload(
                          path = "org_logo_${Clock.System.now().epochSeconds}.jpg",
                          data = file.readBytes()
                        )
                      val imageUrl = bucket.publicUrl(uploadResponse.path)

                      // Cancel the upload progress snackbar
                      snackbarJob.cancel()
                      snackbarHostState.currentSnackbarData?.dismiss()

                      // Update the logo in the ViewModel (this will show success message)
                      updateOrganisationLogo(id, name, imageUrl)
                    } catch (e: Exception) {
                      // Show error message directly here since upload failed before reaching ViewModel
                      snackbarHostState.showSnackbar(
                        message = "❌ Failed to upload logo: ${e.message}",
                        actionLabel = "Close"
                      )
                      println("error uploading files: $e")
                    }
                  }
                }
              }

            Box(contentAlignment = Alignment.Center) {
              AsyncImage(
                model = logo,
                contentDescription = "logo for $name",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(150.dp),
                placeholder =
                  BrushPainter(
                    Brush.linearGradient(
                      listOf(
                        Color(color = 0xFFFFFFFF),
                        Color(color = 0xFFDDDDDD)
                      )
                    )
                  ),
                fallback = painterResource(Res.drawable.baseline_groups),
                error = painterResource(Res.drawable.baseline_groups)
              )

              // Show loading indicator while logo is updating
              if (organisationLogoState.isUpdating) {
                Box(
                  modifier = Modifier
                    .size(150.dp)
                    .background(Color.Black.copy(alpha = 0.5f)),
                  contentAlignment = Alignment.Center
                ) {
                  CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.dp
                  )
                }
              }
            }

            EditImageButton(
              onClick = {
                if (!organisationLogoState.isUpdating) {
                  launcher.launch()
                }
              }
            )
          }
        } else {
          AsyncImage(
            model = logo,
            contentDescription = "logo for $name",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(150.dp),
            placeholder =
              BrushPainter(
                Brush.linearGradient(
                  listOf(
                    Color(color = 0xFFFFFFFF),
                    Color(color = 0xFFDDDDDD)
                  )
                )
              ),
            fallback = painterResource(Res.drawable.baseline_groups),
            error = painterResource(Res.drawable.baseline_groups)
          )
        }
        Text(name, style = MaterialTheme.typography.headlineMedium)
      }
      OrganisationDescription(
        orgId = id,
        description = description!!,
        isLoggedIn = isLoggedIn,
        organisationDescriptionState = organisationDescriptionState,
        onEditModeChange = onDescriptionEditModeChange,
        updateDescription = updateOrganisationDescription
      )
    }

    if (keyPeople.isNotEmpty()) {
      val sortedPeople = keyPeople.sortedBy { it.priority }
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          "कार्यकारिणी/पदाधिकारी",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
        )
        TooltipBox(
          positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
          tooltip = {
            PlainTooltip {
              Text("नए पदाधिकारी जोड़ें")
            }
          },
          state = rememberTooltipState()
        ) {
          // button for adding new organisational members
          IconButton(onClick = { /* Handle icon button click */ }) {
            Icon(Icons.Default.PersonAdd, contentDescription = null)
          }
        }
      }
      FlowRow {
        sortedPeople.forEach { member ->
          KeyPersonItem(
            member = member,
            isLoggedIn = isLoggedIn,
            onUpdatePost = {
              showUpdatePostDialog = member
              updatedPost = member.post
            },
            onDelete = { showDeleteDialog = member },
            isUpdatingPost = memberManagementState.isUpdatingPost,
            isRemoving = memberManagementState.isRemovingMember
          )
        }
      }
    }
  }

  // Delete Confirmation Dialog
  showDeleteDialog?.let { member ->
    AlertDialog(
      onDismissRequest = { showDeleteDialog = null },
      title = { Text("पदाधिकारी हटाएँ?") },
      text = { Text("क्या निश्चितरूप से इन इस पदाधिकारी को हटाना चाहते है?") },
      confirmButton = {
        TextButton(
          onClick = {
            onRemoveMember(member.id, member.member.name, id)
            showDeleteDialog = null
          }
        ) {
          Text("Yes")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteDialog = null }) {
          Text("No")
        }
      }
    )
  }

  // Update Post Dialog
  showUpdatePostDialog?.let { member ->
    AlertDialog(
      onDismissRequest = { showUpdatePostDialog = null },
      title = { Text("पद बदलें") },
      text = {
        Column {
          OutlinedTextField(
            value = updatedPost,
            onValueChange = { updatedPost = it },
            label = { Text("पद") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            if (updatedPost.isNotBlank()) {
              onUpdateMemberPost(member.id, updatedPost, id)
              showUpdatePostDialog = null
            }
          }
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showUpdatePostDialog = null }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
fun OrganisationDescription(
  orgId: String,
  description: String,
  isLoggedIn: Boolean = false,
  organisationDescriptionState: OrganisationDescriptionState,
  onEditModeChange: (Boolean) -> Unit = {},
  updateDescription: (String, String) -> Unit
) {
  val editMode = organisationDescriptionState.editMode
  Column(modifier = Modifier.fillMaxWidth()) {
    if (!editMode) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          modifier = Modifier.weight(1f),
          text = description
        )
        if (isLoggedIn) {
          EditImageButton(
            onClick = { onEditModeChange(true) }
          )
        }
      }
    } else {
      var localText by remember { mutableStateOf(organisationDescriptionState.description) }

      // Update local text when description changes (e.g., on first edit)
      LaunchedEffect(organisationDescriptionState.description) {
        localText = organisationDescriptionState.description
      }

      OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
        value = localText,
        onValueChange = {
          localText = it
        },
        label = { Text("विवरण") }
      )
      Row(
        modifier = Modifier.align(Alignment.End),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        TextButton(
          enabled = !organisationDescriptionState.isUpdating,
          onClick = { onEditModeChange(false) }
        ) {
          Text("Cancel")
        }
        ButtonWithProgressIndicator(
          enabled = !organisationDescriptionState.isUpdating,
          inProgress = organisationDescriptionState.isUpdating
        ) {
          updateDescription(orgId, localText)
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyPersonItem(
  member: OrganisationalMember,
  isLoggedIn: Boolean,
  onUpdatePost: () -> Unit,
  onDelete: () -> Unit,
  isUpdatingPost: Boolean = false,
  isRemoving: Boolean = false
) {
  var showOptionsMenu by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier.padding(4.dp)
  ) {
    Row(
      modifier = Modifier.padding(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      AsyncImage(
        model = member.member.profileImage ?: "",
        contentDescription = "profile image ${member.member.name}",
        contentScale = ContentScale.Crop,
        modifier = Modifier.clip(CircleShape).size(80.dp),
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

      Column(
        modifier = Modifier.padding(12.dp, 8.dp)
      ) {
        Text(member.member.name, style = MaterialTheme.typography.bodyLarge)
        Text(member.post, style = MaterialTheme.typography.bodyMedium)
      }

      if (isLoggedIn) {
        if (isUpdatingPost || isRemoving) {
          // Show loading indicator when operations are in progress
          CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp
          )
        } else {
          Box {
            IconButton(onClick = { showOptionsMenu = true }) {
              Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options"
              )
            }

            DropdownMenu(
              expanded = showOptionsMenu,
              onDismissRequest = { showOptionsMenu = false }
            ) {
              DropdownMenuItem(
                text = { Text("पद बदलें") },
                onClick = {
                  showOptionsMenu = false
                  onUpdatePost()
                },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
              )
              DropdownMenuItem(
                text = { Text("पदाधिकारी को हटाएं") },
                onClick = {
                  showOptionsMenu = false
                  onDelete()
                },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun ButtonWithProgressIndicator(
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  inProgress: Boolean = false,
  onClick: () -> Unit
) {
  Button(
    modifier = modifier,
    onClick = onClick,
    enabled = enabled
  ) {
    if (inProgress) {
      CircularProgressIndicator(
        Modifier.size(20.dp),
        color = MaterialTheme.colorScheme.onPrimary,
        strokeWidth = 2.dp
      )
      Spacer(Modifier.width(8.dp))
    }
    Text("Save")
  }
}
