package org.aryamahasangh.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import org.aryamahasangh.utils.formatShortForBook
import org.aryamahasangh.viewmodel.BookOrder
import org.aryamahasangh.viewmodel.BookOrderViewModel
import org.aryamahasangh.viewmodel.BookOrdersUiState

@Composable
fun ReceivedOrdersScreen(
  viewModel: BookOrderViewModel,
  onNavigateToDetails: (String) -> Unit
) {
//  LaunchedEffect(Unit) {
//    viewModel.loadBookOrders()
//  }
//  ReceivedOrdersContainer(
//    uiState = viewModel.bookOrdersState,
//    onItemClick = { orderId ->
//      // Load the order details and navigate to the details screen
//      viewModel.getBookOrderById(orderId)
//      onNavigateToDetails(orderId)
//    }
//  )
}

@Composable
fun ReceivedOrdersContainer(uiState: StateFlow<BookOrdersUiState>, onItemClick: (String) -> Unit) {
//  val state by uiState.collectAsState()
//  println("ReceivedOrdersContainer: Collected state: $state")
//  val isLoading = state.isLoading
//  val bookOrders = state.bookOrders
//  val error = state.error
//  if(isLoading) {
//    Text("Loading orders...")
//    return
//  }
//
//  if(error != null) {
//    Text("Error: $error")
//    return
//  }
//  println(bookOrders)
//  LazyColumn(
//    modifier = Modifier.fillMaxSize(),
//    contentPadding = PaddingValues(vertical = 8.dp)
//  ) {
//    items(bookOrders, key = { it.id }) { order ->
//      BookOrderListItem(
//        order = order,
//        onClick = onItemClick
//      )
//    }
//  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookOrderListItem(
  order: BookOrder,
  onClick: (String) -> Unit
) {
//  ElevatedCard(
//    onClick = { onClick(order.id) },
//    modifier = Modifier
//      .fillMaxWidth()
//      .padding(horizontal = 8.dp, vertical = 4.dp),
//    shape = RoundedCornerShape(4.dp),
//    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
//  ) {
//    Row(
//      modifier = Modifier.padding(16.dp),
//      verticalAlignment = Alignment.CenterVertically
//    ) {
//      // Optional: Status Indicator (e.g., a colored circle or icon)
//      // Box(modifier = Modifier.size(10.dp).background(if(order.isFulfilled) Color.Green else Color.Yellow, CircleShape))
//      // Spacer(Modifier.width(12.dp))
//
//      Column(modifier = Modifier.weight(1f)) {
//        Text(
//          text = order.fullname,
//          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
//          color = MaterialTheme.colorScheme.onSurfaceVariant
//        )
//        Spacer(modifier = Modifier.height(8.dp))
//
//        InfoRow(icon = Icons.Filled.LocationCity, text = "${order.city}, ${order.pincode}")
//        InfoRow(icon = Icons.Filled.PhoneAndroid, text = order.mobile)
//        InfoRow(icon = Icons.Filled.CalendarToday, text = "Ordered: ${formatShortForBook(order.createdAt)}")
//      }
//
//      // Optional: A small chip for fulfillment status
//      AssistChip(
//        onClick = { /* Can be non-clickable if just for display */ },
//        label = { Text(if (order.isFulfilled) "Fulfilled" else "Pending") },
//        colors = AssistChipDefaults.assistChipColors(
//          containerColor = if (order.isFulfilled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
//          else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
//          labelColor = if (order.isFulfilled) MaterialTheme.colorScheme.onPrimaryContainer
//          else MaterialTheme.colorScheme.onErrorContainer
//        ),
//        border = null // No border for a cleaner look, or AssistChipDefaults.assistChipBorder()
//      )
//    }
//  }
}

fun formatReadableDateTime(isoString: String): String {
  return formatShortForBook(isoString)
}

@Composable
private fun InfoRow(icon: ImageVector, text: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(
      imageVector = icon,
      contentDescription = null, // Decorative
      modifier = Modifier.size(18.dp),
      tint = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = text,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
    )
  }
  Spacer(modifier = Modifier.height(4.dp))
}
