package org.aryamahasangh.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.delay
import org.aryamahasangh.features.activities.Member
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyStaggeredGridState

/**
 * Choice type for MembersComponent
 */
enum class MembersChoiceType {
  MULTIPLE, // Default - allows multiple member selection
  SINGLE    // Allows only single member selection
}

/**
 * Edit modes for MembersComponent
 */
enum class MembersEditMode {
  INDIVIDUAL, // Each member edited individually with dropdown menus
  GROUPED     // All members shown as chips in FlowRow
}

/**
 * Configuration for MembersComponent
 */
data class MembersConfig(
  val label: String = "कार्यकारिणी/पदाधिकारी",
  val addButtonText: String = "पदाधिकारी जोड़ें",
  val postLabel: String = "पद",
  val postPlaceholder: String = "संयोजक, कोषाध्यक्ष इत्यादि",
  val isPostMandatory: Boolean = false,
  val isMandatory: Boolean = false,
  val minMembers: Int = 1,
  val showMemberCount: Boolean = true,
  val editMode: MembersEditMode = MembersEditMode.GROUPED,
  val enableReordering: Boolean = false, // Enable drag-and-drop reordering
  val reorderingHint: String = "खींचकर क्रम बदलें", // Hint text for reordering
  // NEW: Choice type
  val choiceType: MembersChoiceType = MembersChoiceType.MULTIPLE,
  // NEW: Label for single mode
  val singleModeLabel: String = "सदस्य चुनें",
  // NEW: Button text for single mode
  val singleModeButtonText: String = "सदस्य चुनें"
)

/**
 * State for MembersComponent
 */
data class MembersState(
  val members: Map<Member, Pair<String, Int>> = emptyMap() // Member to (Post, Priority)
) {
  val hasMembers: Boolean get() = members.isNotEmpty()
  val memberCount: Int get() = members.size

  fun addMember(member: Member, post: String = "", priority: Int = members.size): MembersState {
    return copy(members = members + (member to Pair(post, priority)))
  }

  fun removeMember(member: Member): MembersState {
    return copy(members = members - member)
  }

  fun updateMemberPost(member: Member, post: String): MembersState {
    val currentPair = members[member]
    return if (currentPair != null) {
      copy(members = members + (member to Pair(post, currentPair.second)))
    } else {
      this
    }
  }

  fun updateMemberPriority(member: Member, priority: Int): MembersState {
    val currentPair = members[member]
    return if (currentPair != null) {
      copy(members = members + (member to Pair(currentPair.first, priority)))
    } else {
      this
    }
  }

  fun reorderMembers(reorderedMembers: List<Member>): MembersState {
    val newMembers = reorderedMembers.mapIndexed { index, member ->
      val currentPair = members[member]
      if (currentPair != null) {
        member to Pair(currentPair.first, index)
      } else {
        member to Pair("", index)
      }
    }.toMap()
    return copy(members = newMembers)
  }

  fun hasChanges(initialState: MembersState): Boolean {
    return this != initialState
  }

  // Get members sorted by priority
  fun getSortedMembers(): List<Member> {
    return members.keys.sortedBy { members[it]?.second ?: 0 }
  }
}

/**
 * Reusable component for managing members
 *
 * @param state Current state of members
 * @param onStateChange Callback when state changes
 * @param config Configuration for the component
 * @param error Error message to display
 * @param searchMembers Function to search members
 * @param allMembers List of all available members
 * @param onTriggerSearch Function to trigger server search
 * @param modifier Modifier for the component
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MembersComponent(
  state: MembersState,
  onStateChange: (MembersState) -> Unit,
  config: MembersConfig = MembersConfig(),
  error: String? = null,
  searchMembers: (String) -> List<Member> = { emptyList() },
  allMembers: List<Member> = emptyList(),
  onTriggerSearch: (String) -> Unit = {},
  modifier: Modifier = Modifier
) {
  var showAddMemberDialog by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
  ) {
    Text(
      text = if (config.choiceType == MembersChoiceType.SINGLE) config.singleModeLabel else config.label,
      style = MaterialTheme.typography.titleMedium,
      modifier = Modifier.padding(bottom = 8.dp)
    )

    // Show reordering hint if enabled and members exist
    if (config.enableReordering && state.hasMembers) {
      Text(
        text = config.reorderingHint,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
      )
    }

    when (config.editMode) {
      MembersEditMode.GROUPED -> {
        if (config.choiceType == MembersChoiceType.SINGLE) {
          // For single mode, show simple member display without role editing
          SingleMemberDisplay(
            state = state,
            onStateChange = onStateChange,
            config = config,
            onShowAddDialog = { showAddMemberDialog = true }
          )
        } else if (config.enableReordering) {
          ReorderableGroupedMembersEditor(
            state = state,
            onStateChange = onStateChange,
            config = config,
            onShowAddDialog = { showAddMemberDialog = true }
          )
        } else {
          GroupedMembersEditor(
            state = state,
            onStateChange = onStateChange,
            config = config,
            onShowAddDialog = { showAddMemberDialog = true }
          )
        }
      }

      MembersEditMode.INDIVIDUAL -> {
        if (config.choiceType == MembersChoiceType.SINGLE) {
          // For single mode, show simple member display
          SingleMemberDisplay(
            state = state,
            onStateChange = onStateChange,
            config = config,
            onShowAddDialog = { showAddMemberDialog = true }
          )
        } else {
          IndividualMembersEditor(
            state = state,
            onStateChange = onStateChange,
            config = config,
            onShowAddDialog = { showAddMemberDialog = true }
          )
        }
      }
    }

    // Add member button and count for INDIVIDUAL mode
    if (config.editMode == MembersEditMode.INDIVIDUAL) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedButton(
          onClick = { showAddMemberDialog = true }
        ) {
          Icon(
            Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize)
          )
          Spacer(Modifier.size(ButtonDefaults.IconSpacing))
          Text(
            text = if (config.choiceType == MembersChoiceType.SINGLE) config.singleModeButtonText else config.addButtonText
          )
        }

        if (config.showMemberCount && state.hasMembers) {
          Text(
            text = "${state.memberCount} ${if (state.memberCount == 1) "पदाधिकारी जोड़ा गया" else "पदाधिकारी जोड़े गए"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }
    }

    // Error message
    error?.let {
      Text(
        text = it,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 4.dp)
      )
    }
  }

  // Add Member Dialog
  if (showAddMemberDialog) {
    MemberSelectionDialog(
      onDismiss = { showAddMemberDialog = false },
      onMembersSelected = { selectedMembers ->
        if (config.choiceType == MembersChoiceType.SINGLE) {
          // For single choice, replace any existing selection with the new one
          if (selectedMembers.isNotEmpty()) {
            val newMember = selectedMembers.first()
            // Clear existing members and add the new one
            val newState = MembersState(
              members = mapOf(newMember to Pair("", 0))
            )
            onStateChange(newState)
          }
          showAddMemberDialog = false
        } else {
          // For multiple choice, keep existing logic
          val existingMemberIds = state.members.keys.map { it.id }.toSet()
          val newMembers = selectedMembers.filter { it.id !in existingMemberIds }

          // Add new members with empty posts and sequential priority
          var newState = state
          newMembers.forEach { member ->
            newState = newState.addMember(member, "", newState.memberCount)
          }
          onStateChange(newState)

          showAddMemberDialog = false
        }
      },
      searchMembers = searchMembers,
      allMembers = allMembers,
      onTriggerSearch = onTriggerSearch,
      choiceType = config.choiceType
    )
  }
}

@Composable
private fun ReorderableGroupedMembersEditor(
  state: MembersState,
  onStateChange: (MembersState) -> Unit,
  config: MembersConfig,
  onShowAddDialog: () -> Unit
) {
  val sortedMembers = state.getSortedMembers()
  val hapticFeedback = LocalHapticFeedback.current

  // Reorderable staggered grid state
  val lazyStaggeredGridState = rememberLazyStaggeredGridState()
  val reorderableLazyStaggeredGridState =
    rememberReorderableLazyStaggeredGridState(lazyStaggeredGridState) { from, to ->
      // Reorder the members list
      val mutableList = sortedMembers.toMutableList()
      val item = mutableList.removeAt(from.index)
      mutableList.add(to.index, item)

      // Update state with new ordering
      onStateChange(state.reorderMembers(mutableList))
      hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

  // Display members in a reorderable staggered grid
  LazyVerticalStaggeredGrid(
    columns = StaggeredGridCells.Adaptive(minSize = 320.dp),
    state = lazyStaggeredGridState,
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 100.dp, max = 600.dp),
    verticalItemSpacing = 8.dp,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    userScrollEnabled = false
  ) {
    items(sortedMembers.size, key = { sortedMembers[it].id }) { index ->
      val member = sortedMembers[index]
      val postPair = state.members[member] ?: Pair("", 0)

      ReorderableItem(reorderableLazyStaggeredGridState, key = member.id) { isDragging ->
        val elevation by animateDpAsState(if (isDragging) 8.dp else 2.dp, label = "drag_elevation")

        Surface(
          shadowElevation = elevation,
          modifier = Modifier.fillMaxWidth()
        ) {
          ReorderableMemberChip(
            member = member,
            post = postPair.first,
            config = config,
            onPostChange = { newPost ->
              onStateChange(state.updateMemberPost(member, newPost))
            },
            onRemove = {
              onStateChange(state.removeMember(member))
            },
            isDragging = isDragging,
            modifier = Modifier.draggableHandle(
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
    }
  }

  Spacer(modifier = Modifier.height(8.dp))

  // Add member button and count
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    OutlinedButton(
      onClick = onShowAddDialog
    ) {
      Icon(
        Icons.Default.Add,
        contentDescription = null,
        modifier = Modifier.size(ButtonDefaults.IconSize)
      )
      Spacer(Modifier.size(ButtonDefaults.IconSpacing))
      Text(config.addButtonText)
    }

    if (config.showMemberCount && state.hasMembers) {
      Text(
        text = "${state.memberCount} ${if (state.memberCount == 1) "पदाधिकारी जोड़ा गया" else "पदाधिकारी जोड़े गए"}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary
      )
    }
  }
}

@Composable
private fun ReorderableMemberChip(
  member: Member,
  post: String,
  config: MembersConfig,
  onPostChange: (String) -> Unit,
  onRemove: () -> Unit,
  isDragging: Boolean,
  modifier: Modifier = Modifier
) {
  var text by remember { mutableStateOf(post) }

  // Update text when post changes externally
  LaunchedEffect(post) {
    text = post
  }

  Card(
    modifier = modifier.padding(4.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isDragging)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
      else
        MaterialTheme.colorScheme.surface
    )
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.Top
    ) {
      // Drag handle icon
      Icon(
        Icons.Default.DragHandle,
        contentDescription = "खींचकर क्रम बदलें",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
      )

      // Member profile image or icon
      if (!member.profileImage.isNullOrEmpty()) {
        AsyncImage(
          model = ImageRequest.Builder(LocalPlatformContext.current)
            .data(member.profileImage)
            .crossfade(true)
            .build(),
          contentDescription = "Profile Image",
          modifier = Modifier.size(32.dp).clip(CircleShape),
          contentScale = ContentScale.Crop
        )
      } else {
        Icon(
          modifier = Modifier.size(32.dp),
          imageVector = Icons.Filled.Face,
          contentDescription = "Profile",
          tint = Color.Gray
        )
      }

      // Member info and role input
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = member.name,
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
          modifier = Modifier.fillMaxWidth(),
          value = text,
          onValueChange = { newText ->
            text = newText
            onPostChange(newText)
          },
          label = { Text(config.postLabel) },
          placeholder = { Text(config.postPlaceholder) },
          textStyle = MaterialTheme.typography.bodySmall,
          isError = config.isPostMandatory && text.isBlank(),
          singleLine = true
        )
      }

      // Remove button
      IconButton(
        onClick = onRemove,
        modifier = Modifier.size(32.dp)
      ) {
        Icon(
          Icons.Default.Close,
          contentDescription = "हटाएं",
          modifier = Modifier.size(20.dp)
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupedMembersEditor(
  state: MembersState,
  onStateChange: (MembersState) -> Unit,
  config: MembersConfig,
  onShowAddDialog: () -> Unit
) {
  // Display Selected Members as Input Chips with inline role editing
  // Always use FlowRow layout (no drag and drop in forms)
  FlowRow(
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    state.members.entries.sortedBy { it.value.second }.forEach { (member, postPair) ->
      var text by remember { mutableStateOf(postPair.first) }
      InputChip(
        selected = true,
        onClick = { /* handled in close button */ },
        label = {
          Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
          ) {
            // Member profile image or icon - aligned to top
            if (!member.profileImage.isNullOrEmpty()) {
              AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                  .data(member.profileImage)
                  .crossfade(true)
                  .build(),
                contentDescription = "Profile Image",
                modifier = Modifier.size(24.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
              )
            } else {
              Icon(
                modifier = Modifier.size(24.dp),
                imageVector = Icons.Filled.Face,
                contentDescription = "Profile",
                tint = Color.Gray
              )
            }

            // Member info and role input
            Column {
              Text(
                text = member.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              OutlinedTextField(
                modifier = Modifier.width(180.dp),
                value = text,
                onValueChange = { newText ->
                  text = newText
                  onStateChange(state.updateMemberPost(member, newText))
                },
                label = { Text(config.postLabel) },
                placeholder = { Text(config.postPlaceholder) },
                textStyle = MaterialTheme.typography.bodySmall,
                isError = config.isPostMandatory && text.isBlank()
              )
            }

            // Remove button - aligned to top
            IconButton(
              onClick = {
                onStateChange(state.removeMember(member))
              },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(
                Icons.Default.Close,
                contentDescription = "हटाएं",
                modifier = Modifier.size(20.dp)
              )
            }
          }
        },
        modifier = Modifier.padding(2.dp)
      )
    }
  }

  // Add member button and count for GROUPED mode
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    OutlinedButton(
      onClick = onShowAddDialog
    ) {
      Icon(
        Icons.Default.Add,
        contentDescription = null,
        modifier = Modifier.size(ButtonDefaults.IconSize)
      )
      Spacer(Modifier.size(ButtonDefaults.IconSpacing))
      Text(config.addButtonText)
    }

    if (config.showMemberCount && state.hasMembers) {
      Text(
        text = "${state.memberCount} ${if (state.memberCount == 1) "पदाधिकारी जोड़ा गया" else "पदाधिकारी जोड़े गए"}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary
      )
    }
  }
}

@Composable
private fun IndividualMembersEditor(
  state: MembersState,
  onStateChange: (MembersState) -> Unit,
  config: MembersConfig,
  onShowAddDialog: () -> Unit
) {
  // For INDIVIDUAL mode, display members in a different layout
  // This would be similar to how OrganisationDetailsView shows members
  Column {
    state.members.entries.sortedBy { it.value.second }.forEach { (member, postPair) ->
      Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Profile image
          if (!member.profileImage.isNullOrEmpty()) {
            AsyncImage(
              model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(member.profileImage)
                .crossfade(true)
                .build(),
              contentDescription = "Profile Image",
              modifier = Modifier.size(48.dp).clip(CircleShape),
              contentScale = ContentScale.Crop
            )
          } else {
            Icon(
              modifier = Modifier.size(48.dp),
              imageVector = Icons.Filled.Face,
              contentDescription = "Profile",
              tint = Color.Gray
            )
          }

          Spacer(modifier = Modifier.width(16.dp))

          // Member details
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = member.name,
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
            Text(
              text = postPair.first.ifEmpty { "पद निर्दिष्ट नहीं" },
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          // Actions
          IconButton(
            onClick = {
              onStateChange(state.removeMember(member))
            }
          ) {
            Icon(
              Icons.Default.Close,
              contentDescription = "हटाएं",
              tint = MaterialTheme.colorScheme.error
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SingleMemberDisplay(
  state: MembersState,
  onStateChange: (MembersState) -> Unit,
  config: MembersConfig,
  onShowAddDialog: () -> Unit
) {
  // Display a single member in a simplified way
  if (state.hasMembers) {
    val member = state.getSortedMembers().first()
    val post = state.members[member]?.first ?: ""

    Card(
      modifier = Modifier.width(500.dp).padding(vertical = 8.dp)
    ) {
      Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Profile image or icon
        if (!member.profileImage.isNullOrEmpty()) {
          AsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
              .data(member.profileImage)
              .crossfade(true)
              .build(),
            contentDescription = "Profile Image",
            modifier = Modifier.size(48.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
          )
        } else {
          Icon(
            modifier = Modifier.size(48.dp),
            imageVector = Icons.Filled.Face,
            contentDescription = "Profile",
            tint = Color.Gray
          )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Member info
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = member.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
          )
        }

        // Remove button
        IconButton(
          onClick = {
            onStateChange(state.removeMember(member))
          }
        ) {
          Icon(
            Icons.Default.Close,
            contentDescription = "हटाएं",
            tint = MaterialTheme.colorScheme.error
          )
        }
      }
    }
  } else {
    // Show empty state when no member is selected in single mode
    Surface(
      modifier = Modifier.width(500.dp),
      color = MaterialTheme.colorScheme.surfaceVariant,
      shape = MaterialTheme.shapes.medium
    ) {
      Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          Icons.Default.Face,
          contentDescription = "No member selected",
          modifier = Modifier.size(48.dp),
          tint = Color.Gray
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
          text = "कोई सदस्य नहीं चुना गया है",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

/**
 * Member selection dialog with support for single and multiple selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberSelectionDialog(
  onDismiss: () -> Unit,
  onMembersSelected: (List<Member>) -> Unit,
  searchMembers: (String) -> List<Member>,
  allMembers: List<Member>,
  onTriggerSearch: (String) -> Unit = {},
  choiceType: MembersChoiceType = MembersChoiceType.MULTIPLE
) {
  var query by remember { mutableStateOf("") }
  var selectedMembers by remember { mutableStateOf<Set<Member>>(emptySet()) }
  var filteredMembers by remember { mutableStateOf(allMembers) }
  var isSearching by remember { mutableStateOf(false) }

  // Debounced search
  LaunchedEffect(query) {
    isSearching = true
    delay(500)

    if (query.isBlank()) {
      filteredMembers = allMembers
    } else {
      // First show local results
      val localResults = allMembers.filter { member ->
        member.name.contains(query, ignoreCase = true) ||
          member.phoneNumber.contains(query, ignoreCase = true) ||
          member.email.contains(query, ignoreCase = true)
      }
      filteredMembers = localResults

      // Trigger server search
      onTriggerSearch(query)

      // Get server results
      try {
        val serverResults = searchMembers(query)
        if (serverResults.isNotEmpty()) {
          filteredMembers = serverResults
        }
      } catch (e: Exception) {
        println("Server search failed: ${e.message}")
      }
    }
    isSearching = false
  }

  androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = MaterialTheme.shapes.large,
      modifier = Modifier
        .width(400.dp)
        .height(600.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp)
      ) {
        Text(
          text = if (choiceType == MembersChoiceType.SINGLE) "सदस्य चुनें" else "पदाधिकारी चुनें",
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
              androidx.compose.material.icons.Icons.Default.Search,
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
        androidx.compose.foundation.lazy.LazyColumn(
          modifier = Modifier.weight(1f)
        ) {
          if (filteredMembers.isEmpty() && query.isNotEmpty() && !isSearching) {
            item {
              Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  "कोई सदस्य नहीं मिला",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          } else {
            items(filteredMembers.size) { index ->
              val member = filteredMembers[index]
              MemberSelectionItem(
                member = member,
                selected = selectedMembers.contains(member),
                choiceType = choiceType,
                onClick = {
                  when (choiceType) {
                    MembersChoiceType.SINGLE -> {
                      selectedMembers = setOf(member)
                      onMembersSelected(listOf(member))
                      onDismiss()
                    }
                    MembersChoiceType.MULTIPLE -> {
                      selectedMembers = if (selectedMembers.contains(member)) {
                        selectedMembers - member
                      } else {
                        selectedMembers + member
                      }
                    }
                  }
                }
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons (only for MULTIPLE mode)
        if (choiceType == MembersChoiceType.MULTIPLE) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            TextButton(onClick = onDismiss) {
              Text("रद्द करें")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = {
                onMembersSelected(selectedMembers.toList())
                onDismiss()
              },
              enabled = selectedMembers.isNotEmpty()
            ) {
              Text("चुनें")
            }
          }
        } else {
          // For single mode, just show cancel button
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            TextButton(onClick = onDismiss) {
              Text("रद्द करें")
            }
          }
        }
      }
    }
  }
}

/**
 * Member selection item for the dialog
 */
@Composable
private fun MemberSelectionItem(
  member: Member,
  selected: Boolean,
  choiceType: MembersChoiceType,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Profile image
    if (!member.profileImage.isNullOrEmpty()) {
      AsyncImage(
        model = ImageRequest.Builder(LocalPlatformContext.current)
          .data(member.profileImage)
          .crossfade(true)
          .build(),
        contentDescription = "Profile Image",
        modifier = Modifier.size(48.dp).clip(CircleShape),
        contentScale = ContentScale.Crop
      )
    } else {
      Icon(
        modifier = Modifier.size(48.dp),
        imageVector = androidx.compose.material.icons.Icons.Filled.Face,
        contentDescription = "Profile",
        tint = Color.Gray
      )
    }

    // Member info
    Column(
      modifier = Modifier
        .weight(1f)
        .padding(start = 12.dp)
    ) {
      Text(
        text = member.name,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
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

    // Selection indicator
    when (choiceType) {
      MembersChoiceType.SINGLE -> {
        RadioButton(
          selected = selected,
          onClick = { onClick() }
        )
      }
      MembersChoiceType.MULTIPLE -> {
        Checkbox(
          checked = selected,
          onCheckedChange = { onClick() }
        )
      }
    }
  }
}

/**
 * Validation function for MembersState
 */
fun validateMembers(
  state: MembersState,
  config: MembersConfig
): String? {
  return when {
    config.isMandatory && state.memberCount < config.minMembers -> {
      if (config.minMembers == 1) {
        "न्यूनतम एक पदाधिकारी जोड़ना आवश्यक है"
      } else {
        "न्यूनतम ${config.minMembers} पदाधिकारी जोड़ना आवश्यक है"
      }
    }

    config.isPostMandatory && state.members.any { it.value.first.isBlank() } -> {
      "सभी पदाधिकारियों के लिए पद निर्दिष्ट करना आवश्यक है"
    }

    else -> null
  }
}
