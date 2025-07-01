package com.aryamahasangh.features.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import aryamahasangh.composeapp.generated.resources.Res
import aryamahasangh.composeapp.generated.resources.family_add
import coil3.compose.AsyncImage
import com.aryamahasangh.navigation.LocalSnackbarHostState
import com.aryamahasangh.utils.WithTooltip
import org.jetbrains.compose.resources.vectorResource

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AryaPariwarListScreen(
  viewModel: FamilyViewModel,
  onNavigateToFamilyDetail: (String) -> Unit = {},
  onNavigateToCreateFamily: () -> Unit = {},
  onEditFamily: (String) -> Unit = {},
  onDeleteFamily: (String) -> Unit = {}
) {
  val uiState by viewModel.familiesUiState.collectAsState()
  val snackbarHostState = LocalSnackbarHostState.current
  val keyboardController = LocalSoftwareKeyboardController.current

  LaunchedEffect(Unit) {
    viewModel.loadFamilies()
  }

  val windowInfo = currentWindowAdaptiveInfo()
  val isCompact = windowInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT

  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = if (isCompact) Arrangement.SpaceBetween else Arrangement.Start
    ) {
      // Search Bar
      OutlinedTextField(
        value = uiState.searchQuery,
        onValueChange = viewModel::searchFamilies,
        modifier = if (isCompact) Modifier.weight(1f) else Modifier.widthIn(max = 600.dp),
        placeholder = { Text("परिवार का नाम") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "खोजें") },
        singleLine = true
      )

      if (!isCompact) {
        Spacer(modifier = Modifier.weight(1f))
      } else {
        Spacer(modifier = Modifier.width(16.dp))
      }

      if (isCompact) {
        // Tooltip for IconButton on compact screens
        WithTooltip(tooltip = "नया परिवार जोड़ें") {
          Box(
            modifier =
              Modifier
                .clip(RectangleShape).clickable { onNavigateToCreateFamily() }
          ) {
            Icon(
              modifier =
                Modifier
                  .size(56.dp).padding(8.dp),
              imageVector = vectorResource(Res.drawable.family_add),
              contentDescription = "नया परिवार जोड़ें"
            )
          }
        }
      } else {
        // Button with text for larger screens
        Button(
          onClick = onNavigateToCreateFamily
        ) {
          Icon(
            modifier =
              Modifier
                .size(24.dp),
            imageVector = vectorResource(Res.drawable.family_add),
            contentDescription = null
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("नया परिवार जोड़ें")
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Loading state
    if (uiState.isLoading) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator()
      }
      return@Column
    }

    // Error state
    uiState.error?.let { error ->
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
          )
      ) {
        Text(
          text = error,
          modifier = Modifier.padding(16.dp),
          color = MaterialTheme.colorScheme.onErrorContainer
        )
      }
      return@Column
    }

    // Families List
    val familiesToShow =
      if (uiState.searchQuery.isNotBlank()) {
        uiState.searchResults
      } else {
        uiState.families
      }

    if (familiesToShow.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = vectorResource(Res.drawable.family_add),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = if (uiState.searchQuery.isNotBlank()) "कोई परिवार नहीं मिला" else "कोई परिवार पंजीकृत नहीं है",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(familiesToShow.chunked(2)) { chunk ->
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            chunk.forEach { family ->
              FamilyItem(
                family = family,
                onFamilyClick = { onNavigateToFamilyDetail(family.id) },
                onEditFamily = { onEditFamily(family.id) },
                onDeleteFamily = { onDeleteFamily(family.id) },
                modifier = Modifier.width(450.dp)
              )
            }
          }
        }
      }
    }
  }
}


@Composable
private fun FamilyItem(
  family: FamilyShort,
  onFamilyClick: () -> Unit,
  onEditFamily: (String) -> Unit,
  onDeleteFamily: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var expanded by remember { mutableStateOf(false) }
  var showDeleteDialog by remember { mutableStateOf(false) }

  // Create a tinted painter for error state
  val vectorPainter = rememberVectorPainter(Icons.Default.FamilyRestroom)
  val errorPainter = remember(vectorPainter) {
    object : Painter() {
      override val intrinsicSize: Size get() = vectorPainter.intrinsicSize

      override fun DrawScope.onDraw() {
        with(vectorPainter) {
          draw(
            size = size,
            colorFilter = ColorFilter.tint(Color.Gray)
          )
        }
      }
    }
  }

  ElevatedCard(
    modifier =
      modifier
        .clickable { onFamilyClick() }
        .width(500.dp),
    shape = RoundedCornerShape(8.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Family Photo (square shape)
      val familyPhoto = family.photos.firstOrNull()
      if (familyPhoto != null) {
        AsyncImage(
          model = familyPhoto,
          contentDescription = "परिवार फोटो",
          modifier =
            Modifier
              .size(80.dp)
              .clip(RoundedCornerShape(8.dp)),
          contentScale = ContentScale.Crop,
          error = errorPainter,
          fallback = errorPainter
        )
      } else {
        // Placeholder for family photo
        Surface(
          modifier = Modifier.size(80.dp),
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.surfaceVariant
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
          ) {
            Icon(
              Icons.Default.FamilyRestroom,
              contentDescription = "परिवार फोटो",
              modifier = Modifier.size(40.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(16.dp))

      // Family Info
      Column(
        modifier = Modifier.weight(1f)
      ) {
        Text(
          text = family.name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Medium
        )

        if (family.address.isNotEmpty()) {
          Text(
            text = family.address,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
          )
        }

        if (family.aryaSamajName.isNotEmpty()) {
          Text(
            text = "आर्य समाज: ${family.aryaSamajName}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 2.dp)
          )
        }
      }

      // Overflow menu
      Box {
        IconButton(onClick = { expanded = !expanded }) {
          Icon(Icons.Default.MoreVert, contentDescription = "अधिक क्रियाएँ")
        }

        DropdownMenu(
          expanded = expanded,
          onDismissRequest = { expanded = false }
        ) {
          DropdownMenuItem(
            text = { Text("संपादित करें") },
            onClick = {
              expanded = false
              onEditFamily(family.id)
            },
            leadingIcon = {
              Icon(Icons.Default.Edit, contentDescription = "संपादित करें")
            }
          )
          DropdownMenuItem(
            text = { Text("हटाएँ") },
            onClick = {
              expanded = false
              showDeleteDialog = true
            },
            leadingIcon = {
              Icon(
                Icons.Default.Delete,
                contentDescription = "हटाएँ",
                tint = MaterialTheme.colorScheme.error
              )
            }
          )
        }
      }
    }
  }

  // Delete confirmation dialog
  if (showDeleteDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteDialog = false },
      title = { Text("परिवार हटाएँ") },
      text = {
        Text("क्या आप वाकई \"${family.name}\" परिवार को हटाना चाहते हैं? यह कार्रवाई पूर्ववत नहीं की जा सकती।")
      },
      confirmButton = {
        TextButton(
          onClick = {
            showDeleteDialog = false
            onDeleteFamily(family.id)
          },
          colors =
            ButtonDefaults.textButtonColors(
              contentColor = MaterialTheme.colorScheme.error
            )
        ) {
          Text("हटाएँ")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteDialog = false }) {
          Text("रद्द करें")
        }
      }
    )
  }
}
