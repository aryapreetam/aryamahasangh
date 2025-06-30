package com.aryamahasangh.screens

import androidx.compose.runtime.Composable
import com.aryamahasangh.viewmodel.BookOrderViewModel

@Composable
fun BookOrderDetailsScreen(
  viewModel: BookOrderViewModel,
  orderId: String,
  onBackClick: () -> Unit
) {
  // Load the order details when the screen is first displayed
//    LaunchedEffect(orderId) {
//        viewModel.getBookOrderById(orderId)
//    }
//
//    val detailsState by viewModel.bookOrderDetailsState.collectAsState()
//
//    BookOrderDetailsContent(
//        uiState = detailsState,
//        onBackClick = onBackClick
//    )
}
//
// @OptIn(ExperimentalMaterial3Api::class)
// @Composable
// fun BookOrderDetailsContent(
//    uiState: BookOrderDetailsUiState,
//    onBackClick: () -> Unit
// ) {
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(8.dp)
//    ) {
//        if (uiState.isLoading) {
//            CircularProgressIndicator(
//                modifier = Modifier.align(Alignment.Center)
//            )
//        } else if (uiState.error != null) {
//            Column(
//                modifier = Modifier
//                    .align(Alignment.Center)
//                    .padding(16.dp),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                Icon(
//                    imageVector = Icons.Default.Error,
//                    contentDescription = null,
//                    tint = MaterialTheme.colorScheme.error
//                )
//                Spacer(modifier = Modifier.height(8.dp))
//                Text(
//                    text = "Error: ${uiState.error}",
//                    color = MaterialTheme.colorScheme.error
//                )
//                Spacer(modifier = Modifier.height(16.dp))
//                Button(onClick = onBackClick) {
//                    Text("Go Back")
//                }
//            }
//        } else if (uiState.bookOrder != null) {
//            val order = uiState.bookOrder
//            Column(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .verticalScroll(rememberScrollState())
//                    .padding(16.dp)
//            ) {
//                // Order ID and Status
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Text(
//                        text = "Order #${order.id.takeLast(6).uppercase()}",
//                        style = MaterialTheme.typography.titleLarge,
//                        fontWeight = FontWeight.Bold
//                    )
//                    AssistChip(
//                        onClick = { /* Non-clickable */ },
//                        label = { Text(if (order.isFulfilled) "Fulfilled" else "Pending") },
//                        colors = AssistChipDefaults.assistChipColors(
//                            containerColor = if (order.isFulfilled)
//                                MaterialTheme.colorScheme.primaryContainer
//                            else
//                                MaterialTheme.colorScheme.errorContainer,
//                            labelColor = if (order.isFulfilled)
//                                MaterialTheme.colorScheme.onPrimaryContainer
//                            else
//                                MaterialTheme.colorScheme.onErrorContainer
//                        )
//                    )
//                }
//
//                Spacer(modifier = Modifier.height(24.dp))
//
//                // Customer Information
//                SectionTitle(text = "Customer Information")
//                DetailItem(
//                    icon = Icons.Default.Person,
//                    label = "Name",
//                    value = order.fullname
//                )
//                DetailItem(
//                    icon = Icons.Default.Phone,
//                    label = "Mobile",
//                    value = order.mobile
//                )
//
//                Spacer(modifier = Modifier.height(24.dp))
//
//                // Address Information
//                SectionTitle(text = "Shipping Address")
//                DetailItem(
//                    icon = Icons.Default.Home,
//                    label = "Address",
//                    value = order.address
//                )
//                DetailItem(
//                    icon = Icons.Default.LocationCity,
//                    label = "City",
//                    value = order.city
//                )
//                DetailItem(
//                    icon = Icons.Default.Place,
//                    label = "District",
//                    value = order.district
//                )
//                DetailItem(
//                    icon = Icons.Default.Map,
//                    label = "State",
//                    value = order.state
//                )
//                DetailItem(
//                    icon = Icons.Default.Pin,
//                    label = "Pincode",
//                    value = order.pincode
//                )
//                DetailItem(
//                    icon = Icons.Default.Public,
//                    label = "Country",
//                    value = order.country
//                )
//
//                Spacer(modifier = Modifier.height(24.dp))
//
//                // District Officer Information
//                SectionTitle(text = "District Officer Information")
//                DetailItem(
//                    icon = Icons.Default.Person,
//                    label = "Name",
//                    value = order.districtOfficerName
//                )
//                DetailItem(
//                    icon = Icons.Default.Phone,
//                    label = "Mobile",
//                    value = order.districtOfficerNumber
//                )
//
//                Spacer(modifier = Modifier.height(24.dp))
//
//                // Order Information
//                SectionTitle(text = "Order Information")
//                DetailItem(
//                    icon = Icons.Default.CalendarToday,
//                    label = "Order Date",
//                    value = formatShortForBook(order.createdAt)
//                )
//
//                // Uncomment if updatedAt field is available in the API response
//                // if (order.updatedAt != null) {
//                //     DetailItem(
//                //         icon = Icons.Default.Update,
//                //         label = "Last Updated",
//                //         value = formatShort(order.updatedAt)
//                //     )
//                // }
//
//                Spacer(modifier = Modifier.height(32.dp))
//            }
//        } else {
//            // No order loaded yet
//            Text(
//                text = "No order details available",
//                modifier = Modifier
//                    .align(Alignment.Center)
//                    .padding(16.dp)
//            )
//        }
//    }
// }
//
// @Composable
// fun SectionTitle(text: String) {
//    Text(
//        text = text,
//        style = MaterialTheme.typography.titleMedium,
//        fontWeight = FontWeight.Bold,
//        color = MaterialTheme.colorScheme.primary,
//        modifier = Modifier.padding(bottom = 8.dp)
//    )
// }
//
// @Composable
// fun DetailItem(
//    icon: ImageVector,
//    label: String,
//    value: String
// ) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 4.dp),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Icon(
//            imageVector = icon,
//            contentDescription = null,
//            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
//            modifier = Modifier.size(24.dp)
//        )
//        Spacer(modifier = Modifier.width(16.dp))
//        Column {
//            Text(
//                text = label,
//                style = MaterialTheme.typography.bodySmall,
//                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
//            )
//            Text(
//                text = value,
//                style = MaterialTheme.typography.bodyLarge
//            )
//        }
//    }
// }

@Composable
fun BookOrderDetailsPreview() {
}
