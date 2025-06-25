package org.aryamahasangh.features.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.aryamahasangh.features.admin.data.AryaSamajListItem
import org.aryamahasangh.features.admin.data.AryaSamajViewModel
import org.aryamahasangh.utils.WithTooltip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AryaSamajListScreen(
  viewModel: AryaSamajViewModel,
  onNavigateToAddAryaSamaj: () -> Unit = {},
  onNavigateToAryaSamajDetail: (String) -> Unit = {},
  onEditAryaSamaj: (String) -> Unit = {} // Updated parameter type
) {
  val listUiState by viewModel.listUiState.collectAsState()

  LaunchedEffect(Unit) {
    viewModel.loadAryaSamajs()
  }

  Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
    // Search bar and add button
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      OutlinedTextField(
        value = listUiState.searchQuery,
        onValueChange = viewModel::searchAryaSamajs,
        modifier = Modifier.weight(1f),
        placeholder = { Text("आर्य समाज खोजें") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
        singleLine = true
      )

      // button to add new arya samaj with tooltip
      WithTooltip(tooltip = "नया आर्य समाज जोड़ें") {
        IconButton(
          onClick = onNavigateToAddAryaSamaj
        ) {
          Icon(
            Icons.Default.AddHomeWork,
            contentDescription = "नया आर्य समाज जोड़ें"
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Handle loading state
    if (listUiState.isLoading) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator()
      }
      return
    }

    // Handle error state
    listUiState.error?.let { error ->
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
      return
    }

    // Arya Samaj list
    val filteredAryaSamajs =
      if (listUiState.searchQuery.isBlank()) {
        listUiState.aryaSamajs
      } else {
        listUiState.aryaSamajs.filter {
          it.name.contains(listUiState.searchQuery, ignoreCase = true)
        }
      }

    if (filteredAryaSamajs.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            Icons.Default.AddHomeWork,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text =
              if (listUiState.searchQuery.isBlank()) {
                "कोई आर्य समाज नहीं मिला"
              } else {
                "\"${listUiState.searchQuery}\" के लिए कोई परिणाम नहीं मिला"
              },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(filteredAryaSamajs) { aryaSamaj ->
          AryaSamajListItem(
            aryaSamaj = aryaSamaj,
            onItemClick = { onNavigateToAryaSamajDetail(aryaSamaj.id) },
            onDeleteClick = { viewModel.deleteAryaSamaj(aryaSamaj.id) },
            onEditClick = { onEditAryaSamaj(aryaSamaj.id) } // Updated to pass the id
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AryaSamajListItem(
  aryaSamaj: AryaSamajListItem,
  onItemClick: () -> Unit,
  onDeleteClick: () -> Unit,
  onEditClick: () -> Unit
) {
  var showDeleteDialog by remember { mutableStateOf(false) }
  var showDropdownMenu by remember { mutableStateOf(false) }

  Card(
    onClick = onItemClick,
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp)
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.Top
    ) {
      Column(
        modifier = Modifier.weight(1f)
      ) {
        Text(
          text = aryaSamaj.name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = aryaSamaj.formattedAddress,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }

      Box {
        IconButton(onClick = { showDropdownMenu = true }) {
          Icon(
            Icons.Default.MoreVert,
            contentDescription = "विकल्प"
          )
        }

        DropdownMenu(
          expanded = showDropdownMenu,
          onDismissRequest = { showDropdownMenu = false }
        ) {
          DropdownMenuItem(
            text = { Text("संपादित करें") },
            onClick = {
              showDropdownMenu = false
              onEditClick()
            },
            leadingIcon = {
              Icon(
                Icons.Default.Edit,
                contentDescription = null
              )
            }
          )
          DropdownMenuItem(
            text = { Text("हटाएँ") },
            onClick = {
              showDropdownMenu = false
              showDeleteDialog = true
            },
            leadingIcon = {
              Icon(
                Icons.Default.Delete,
                contentDescription = null,
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
      title = { Text("आर्य समाज मिटाएं") },
      text = {
        Text("क्या आप वाकई \"${aryaSamaj.name}\" को मिटाना चाहते हैं? यह कार्रवाई पूर्ववत नहीं की जा सकती।")
      },
      confirmButton = {
        TextButton(
          onClick = {
            showDeleteDialog = false
            onDeleteClick()
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
