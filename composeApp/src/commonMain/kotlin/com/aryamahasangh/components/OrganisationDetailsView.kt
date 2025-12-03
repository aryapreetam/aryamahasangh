package com.aryamahasangh.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import aryamahasangh.composeapp.generated.resources.Res
import aryamahasangh.composeapp.generated.resources.baseline_groups
import aryamahasangh.composeapp.generated.resources.error_profile_image
import coil3.compose.AsyncImage
import com.aryamahasangh.LocalIsAuthenticated
import com.aryamahasangh.features.activities.Member
import com.aryamahasangh.features.organisations.MemberManagementState
import com.aryamahasangh.features.organisations.OrganisationDescriptionState
import com.aryamahasangh.features.organisations.OrganisationLogoState
import com.aryamahasangh.features.organisations.OrganisationalMember
import com.aryamahasangh.navigation.LocalSnackbarHostState
import com.aryamahasangh.screens.EditImageButton
import com.aryamahasangh.util.ImageCompressionService
import com.aryamahasangh.utils.FileUploadUtils
import com.aryamahasangh.util.Result
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import org.koin.compose.koinInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyStaggeredGridState

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
  id: String,
  name: String,
  logo: String?,
  description: String? = null,
  keyPeople: List<OrganisationalMember> = emptyList(),
  isLoggedIn: Boolean,
  organisationDescriptionState: OrganisationDescriptionState = OrganisationDescriptionState(),
  onDescriptionEditModeChange: (Boolean) -> Unit = { },
  updateOrganisationDescription: (String, String) -> Unit = { _, _ -> },
  updateOrganisationLogo: (String, String, String) -> Unit = { _, _, _ -> },
  organisationLogoState: OrganisationLogoState = OrganisationLogoState(),
  onRemoveMember: (String, String, String) -> Unit = { _, _, _ -> },
  onUpdateMemberPost: (String, String, String) -> Unit = { _, _, _ -> },
  onUpdateMemberPriority: (String, Int) -> Unit = { _, _ -> },
  onUpdateMemberPriorities: (List<Pair<String, Int>>) -> Unit = { _ -> },
  memberManagementState: MemberManagementState = MemberManagementState(),
  onAddMemberToOrganisation: (String, String, String, Int) -> Unit = { _, _, _, _ -> },
  searchMembers: (String) -> List<Member> = { emptyList() },
  allMembers: List<Member> = emptyList(),
  onTriggerSearch: (String) -> Unit = {}
) {
  val isLoggedIn = LocalIsAuthenticated.current
  val fileUploadUtils = koinInject<FileUploadUtils>()
  var showDeleteDialog by remember { mutableStateOf<OrganisationalMember?>(null) }
  var showUpdatePostDialog by remember { mutableStateOf<OrganisationalMember?>(null) }
  var updatedPost by remember { mutableStateOf("") }
  var showAddMemberDialog by remember { mutableStateOf(false) }
  var newMembersWithPosts by remember { mutableStateOf<Map<Member, String>>(emptyMap()) }

  // State for priority editing mode
  var editPriorityMode by remember { mutableStateOf(false) }
  var isUpdatingPriorities by remember { mutableStateOf(false) }

  // State for reorderable members list
  var membersList by remember(keyPeople) {
    mutableStateOf(keyPeople.sortedBy { it.priority })
  }

  // Haptic feedback for reordering
  val hapticFeedback = LocalHapticFeedback.current

  // Reorderable staggered grid state for priority editing
  val lazyStaggeredGridState = rememberLazyStaggeredGridState()
  val reorderableLazyStaggeredGridState =
    rememberReorderableLazyStaggeredGridState(lazyStaggeredGridState) { from, to ->
      membersList =
        membersList.toMutableList().apply {
          add(to.index, removeAt(from.index))
        }
      hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

  // Update local list when keyPeople changes (from API updates)
  LaunchedEffect(keyPeople) {
    membersList = keyPeople.sortedBy { it.priority }
  }

  // Functions for priority editing
  fun startPriorityEdit() {
    editPriorityMode = true
  }

  fun cancelPriorityEdit() {
    editPriorityMode = false
    membersList = keyPeople.sortedBy { it.priority } // Reset to original order
  }

  fun savePriorityChanges() {
    isUpdatingPriorities = true

    // Calculate new priorities for all affected members
    val priorityUpdates = mutableListOf<Pair<String, Int>>()

    membersList.forEachIndexed { index, member ->
      val newPriority = index
      // Only update if priority actually changed from original
      val originalMember = keyPeople.find { it.id == member.id }
      if (originalMember != null && originalMember.priority != newPriority) {
        priorityUpdates.add(Pair(member.id, newPriority))
      }
    }

    // Only call API if there are actual priority changes
    if (priorityUpdates.isNotEmpty()) {
      onUpdateMemberPriorities(priorityUpdates)
    }

    editPriorityMode = false
    isUpdatingPriorities = false
  }

  Box(modifier = Modifier.fillMaxSize()) {
    // Main content - Replace Column with LazyColumn to avoid nested scrolling
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(8.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Logo and description section
      item {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
          ) {
            if (isLoggedIn) {
              Row(modifier = Modifier.padding()) {
                val scope = rememberCoroutineScope()
                val snackbarHostState = LocalSnackbarHostState.current ?: return@Row
                val launcher =
                  rememberFilePickerLauncher(
                    type = FileKitType.File(extensions = listOf("png", "jpg", "jpeg", "webp")),
                    mode = FileKitMode.Single,
                    title = "Select logo"
                  ) { file ->
                    if (file != null) {
                      scope.launch {
                        try {
                          // Show immediate upload feedback
                          val snackbarJob =
                            launch {
                              snackbarHostState.showSnackbar(
                                message = "🔄 Uploading new logo...",
                                duration = SnackbarDuration.Indefinite
                              )
                            }

                          // Compress image to 50KB for organization logo
                          val compressedBytes = ImageCompressionService.compressThumbnail(
                            file = file
                          )

                          val uploadResponse =
                            // bucket.upload(
                            //   path = "org_logo_${Clock.System.now().epochSeconds}.webp",
                            //   data = compressedBytes
                            // )
                            fileUploadUtils.uploadBytes(
                              path = "org_logo_${Clock.System.now().epochSeconds}.webp",
                              data = compressedBytes
                            )
                          val imageUrl = when (uploadResponse) {
                            is Result.Success -> uploadResponse.data
                            is Result.Error -> throw Exception(uploadResponse.message)
                            else -> throw Exception("अज्ञात त्रुटि")
                          }
                          // val imageUrl = bucket.publicUrl(uploadResponse.path)

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
                      modifier =
                        Modifier
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
      }

      // Members section header
      if (keyPeople.isNotEmpty() || isLoggedIn) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              "कार्यकारिणी/पदाधिकारी",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
            )
            if (isLoggedIn) {
              TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = {
                  PlainTooltip {
                    Text("नए पदाधिकारी जोड़ें")
                  }
                },
                state = rememberTooltipState()
              ) {
                IconButton(onClick = { showAddMemberDialog = true }) {
                  Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                  )
                }
              }
            }
          }
        }

        // Priority editing mode completion button
        if (editPriorityMode) {
          item {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedButton(
                onClick = { cancelPriorityEdit() },
                enabled = !isUpdatingPriorities
              ) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("निरस्त करें")
              }

              Button(
                onClick = { savePriorityChanges() },
                enabled = !isUpdatingPriorities
              ) {
                if (isUpdatingPriorities) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                  )
                  Spacer(Modifier.width(8.dp))
                } else {
                  Icon(Icons.Default.Check, contentDescription = null)
                  Spacer(Modifier.width(4.dp))
                }
                Text("क्रम परिवर्तन पूर्ण हुआ")
              }
            }
          }
        }

        // Members staggered grid - now as a LazyColumn item
        item {
          LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(minSize = 300.dp),
            state = lazyStaggeredGridState,
            modifier =
              Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp, max = 800.dp),
            // Constrain height but allow flexibility
            verticalItemSpacing = 8.dp,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false // Disable scrolling since we're in a LazyColumn
          ) {
            // Members items
            items(membersList.size, key = { membersList[it].id }) { index ->
              val member = membersList[index]

              if (editPriorityMode) {
                ReorderableItem(reorderableLazyStaggeredGridState, key = member.id) { isDragging ->
                  val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "drag_elevation")

                  Surface(
                    shadowElevation = elevation,
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    KeyPersonItem(
                      member = member,
                      isLoggedIn = isLoggedIn,
                      editPriorityMode = editPriorityMode,
                      onStartPriorityEdit = { startPriorityEdit() },
                      onUpdatePost = {
                        showUpdatePostDialog = member
                        updatedPost = member.post
                      },
                      onDelete = { showDeleteDialog = member },
                      isUpdatingPost = memberManagementState.isUpdatingPost,
                      isRemoving = memberManagementState.isRemovingMember,
                      isDragHandle = true,
                      modifier =
                        Modifier.draggableHandle(
                          onDragStarted = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                          },
                          onDragStopped = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
                          }
                        )
                    )
                  }
                }
              } else {
                KeyPersonItem(
                  member = member,
                  isLoggedIn = isLoggedIn,
                  editPriorityMode = editPriorityMode,
                  onStartPriorityEdit = { startPriorityEdit() },
                  onUpdatePost = {
                    showUpdatePostDialog = member
                    updatedPost = member.post
                  },
                  onDelete = { showDeleteDialog = member },
                  isUpdatingPost = memberManagementState.isUpdatingPost,
                  isRemoving = memberManagementState.isRemovingMember,
                  isDragHandle = false
                )
              }
            }

            // Show new members being added with post input (only in normal mode)
            if (!editPriorityMode) {
              items(newMembersWithPosts.size, key = { newMembersWithPosts.keys.toList()[it].id }) { index ->
                val (member, post) = newMembersWithPosts.toList()[index]
                NewMemberPostInput(
                  member = member,
                  post = post,
                  onPostChange = { newPost ->
                    newMembersWithPosts =
                      newMembersWithPosts.toMutableMap().apply {
                        this[member] = newPost
                      }
                  },
                  onConfirm = {
                    if (post.isNotBlank()) {
                      val maxPriority = keyPeople.maxOfOrNull { it.priority } ?: 0
                      val priority = maxPriority + 1
                      onAddMemberToOrganisation(member.id, post, id, priority)
                      newMembersWithPosts = newMembersWithPosts - member
                    }
                  },
                  onCancel = {
                    newMembersWithPosts = newMembersWithPosts - member
                  },
                  isLoading = memberManagementState.isAddingMember
                )
              }
            }
          }
        }
      }
    }
  }

  // Add Member Dialog
  if (showAddMemberDialog) {
    AddMemberDialog(
      onDismiss = { showAddMemberDialog = false },
      onMembersSelected = { selectedMembers ->
        // Filter out members that are already part of the organisation
        val existingMemberIds = keyPeople.map { it.member.id }.toSet()
        val newMembers = selectedMembers.filter { it.id !in existingMemberIds }

        // Add new members to the post input map
        val newMembersMap = newMembers.associateWith { "" }
        newMembersWithPosts = newMembersWithPosts + newMembersMap

        showAddMemberDialog = false
      },
      searchMembers = searchMembers,
      allMembers = allMembers,
      onTriggerSearch = onTriggerSearch
    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyPersonItem(
  member: OrganisationalMember,
  isLoggedIn: Boolean,
  editPriorityMode: Boolean,
  onStartPriorityEdit: () -> Unit,
  onUpdatePost: () -> Unit,
  onDelete: () -> Unit,
  isUpdatingPost: Boolean = false,
  isRemoving: Boolean = false,
  isDragHandle: Boolean = false,
  modifier: Modifier = Modifier
) {
  var showOptionsMenu by remember { mutableStateOf(false) }

  Row(
    modifier =
      Modifier
        .padding(8.dp)
        .then(modifier),
    verticalAlignment = Alignment.CenterVertically
  ) {
    AsyncImage(
      model = member.member.profileImage ?: "",
      contentDescription = "profile image ${member.member.name}",
      contentScale = ContentScale.Crop,
      modifier =
        Modifier
          .clip(CircleShape)
          .size(80.dp),
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
        CircularProgressIndicator(
          modifier = Modifier.size(24.dp),
          strokeWidth = 2.dp
        )
      } else {
        if (editPriorityMode && isDragHandle) {
          Icon(
            Icons.Default.DragHandle,
            contentDescription = "Drag to reorder",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(8.dp)
          )
        } else if (!editPriorityMode) {
          Box {
            IconButton(onClick = { showOptionsMenu = true }) {
              Icon(
                Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = MaterialTheme.colorScheme.primary
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
                leadingIcon = {
                  Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                  )
                }
              )
              DropdownMenuItem(
                text = { Text("क्रम परिवर्तन") },
                onClick = {
                  showOptionsMenu = false
                  onStartPriorityEdit()
                },
                leadingIcon = {
                  Icon(
                    Icons.Default.SwapVert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                  )
                }
              )
              DropdownMenuItem(
                text = { Text("पदाधिकारी को हटाएं") },
                onClick = {
                  showOptionsMenu = false
                  onDelete()
                },
                leadingIcon = {
                  Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                  )
                }
              )
            }
          }
        }
      }
    }
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
          inProgress = organisationDescriptionState.isUpdating,
          onClick = {
            updateDescription(orgId, localText)
          }
        )
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddMemberDialog(
  onDismiss: () -> Unit,
  onMembersSelected: (List<Member>) -> Unit,
  searchMembers: (String) -> List<Member>,
  allMembers: List<Member>,
  onTriggerSearch: (String) -> Unit = {}
) {
  var query by remember { mutableStateOf("") }
  var selectedMembers by remember { mutableStateOf<Set<Member>>(emptySet()) }
  var filteredMembers by remember { mutableStateOf(allMembers) }
  var isSearching by remember { mutableStateOf(false) }

  // Debounced search with hybrid approach
  LaunchedEffect(query) {
    isSearching = true
    delay(1000)

    if (query.isBlank()) {
      filteredMembers = allMembers
    } else {
      // First show local results immediately for fast UX
      val localResults =
        allMembers.filter { member ->
          member.name.contains(query, ignoreCase = true) ||
            member.phoneNumber.contains(query, ignoreCase = true) ||
            member.email.contains(query, ignoreCase = true)
        }
      filteredMembers = localResults

      // Trigger server search for fresh results
      onTriggerSearch(query)

      // Small delay to allow server search to complete
      delay(500)

      // Get fresh server results
      try {
        val serverResults = searchMembers(query)
        if (serverResults.isNotEmpty()) {
          // Update with server results if we got any
          filteredMembers = serverResults
        }
        // If server returns empty but we have local results, keep local results
        // This handles cases where server might be slower or return different results
      } catch (e: Exception) {
        // If server search fails, keep the local results
        // User still gets some results instead of empty list
        println("Server search failed, using local results: ${e.message}")
      }
    }
    isSearching = false
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = MaterialTheme.shapes.large,
      modifier =
        Modifier
          .width(400.dp)
          .height(600.dp)
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .padding(16.dp)
      ) {
        Text(
          text = "नए पदाधिकारी जोड़ें",
          style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Search bar
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          placeholder = { Text("आर्य का नाम/दूरभाष") },
          leadingIcon = {
            Icon(
              Icons.Default.Search,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary
            )
          },
          trailingIcon = {
            if (isSearching) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
              )
            }
          },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        // List of members
        LazyColumn(
          modifier = Modifier.weight(1f)
        ) {
          if (filteredMembers.isEmpty() && query.isNotEmpty() && !isSearching) {
            item {
              Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  "इस नाम/दूरभाष से संबधित कोई आर्य उपलब्ध नहीं है।",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          } else {
            items(filteredMembers.size) { index ->
              val member = filteredMembers[index]
              MemberListItem(
                member = member,
                selected = selectedMembers.contains(member),
                onClick = {
                  selectedMembers =
                    if (selectedMembers.contains(member)) {
                      selectedMembers - member
                    } else {
                      selectedMembers + member
                    }
                }
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(onClick = onDismiss) {
            Text("बंद करें")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              onMembersSelected(selectedMembers.toList())
            },
            enabled = selectedMembers.isNotEmpty()
          ) {
            Text("पदाधिकारी जोड़ें")
          }
        }
      }
    }
  }
}

@Composable
fun MemberListItem(
  member: Member,
  selected: Boolean,
  onClick: () -> Unit
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .clickable { onClick() }
        .padding(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Profile image on the left
    AsyncImage(
      model = member.profileImage.ifEmpty { null },
      contentDescription = "Profile image of ${member.name}",
      modifier =
        Modifier
          .size(48.dp)
          .clip(CircleShape),
      contentScale = ContentScale.Crop,
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

    // Member info in the middle
    Column(
      modifier =
        Modifier
          .weight(1f)
          .padding(start = 12.dp)
    ) {
      Text(
        text = member.name,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      if (member.email.isNotEmpty()) {
        Text(
          text = member.email,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    // Selection indicator on the right
    Checkbox(
      checked = selected,
      onCheckedChange = { onClick() }
    )
  }
}

@Composable
fun NewMemberPostInput(
  member: Member,
  post: String,
  onPostChange: (String) -> Unit,
  onConfirm: () -> Unit,
  onCancel: () -> Unit,
  isLoading: Boolean = false
) {
  Card(
    modifier = Modifier.padding(8.dp)
  ) {
    Column(
      modifier = Modifier.padding(16.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        AsyncImage(
          model = member.profileImage ?: "",
          contentDescription = "profile image ${member.name}",
          contentScale = ContentScale.Crop,
          modifier =
            Modifier
              .clip(CircleShape)
              .size(48.dp),
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
          modifier =
            Modifier
              .weight(1f)
              .padding(start = 12.dp)
        ) {
          Text(
            text = member.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = post,
            onValueChange = onPostChange,
            label = { Text("पद लिखें") },
            placeholder = { Text("अध्यक्ष, सचिव, कोषाध्यक्ष") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
        }

        // Cancel button
        IconButton(
          onClick = onCancel,
          modifier = Modifier.padding(start = 8.dp)
        ) {
          Icon(
            Icons.Default.Close,
            contentDescription = "Cancel",
            tint = MaterialTheme.colorScheme.primary
          )
        }
      }

      // Confirm button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        Button(
          onClick = onConfirm,
          enabled = post.isNotBlank() && !isLoading,
          modifier = Modifier.padding(top = 8.dp)
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
          }
          Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("जोड़ें")
        }
      }
    }
  }
}
