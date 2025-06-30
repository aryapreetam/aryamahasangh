package com.aryamahasangh.screens

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import com.aryamahasangh.viewmodel.AdmissionsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReceivedApplicationsScreen(viewModel: AdmissionsViewModel) {
//  val scope = rememberCoroutineScope()
//  val snackbarHostState = LocalSnackbarHostState.current
//
//  // Collect UI state from ViewModel
//  val uiState by viewModel.uiState.collectAsState()
//
//  // Handle loading state
//  if (uiState.isLoading) {
//    Box(
//      modifier = Modifier.fillMaxSize(),
//      contentAlignment = Alignment.Center
//    ) {
//      LinearProgressIndicator()
//    }
//    return
//  }
//
//  // Handle error state
//  uiState.error?.let { error ->
//    LaunchedEffect(error) {
//      snackbarHostState.showSnackbar(
//        message = error,
//        actionLabel = "Retry"
//      )
//    }
//
//    Box(
//      modifier = Modifier.fillMaxSize(),
//      contentAlignment = Alignment.Center
//    ) {
//      Column(
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.spacedBy(8.dp)
//      ) {
//        Text("Failed to load student applications")
//        Button(onClick = { viewModel.loadStudentApplications() }) {
//          Text("Retry")
//        }
//      }
//    }
//    return
//  }
//
//  // Handle empty state
//  if (uiState.studentApplications.isEmpty()) {
//    Box(
//      modifier = Modifier.fillMaxSize(),
//      contentAlignment = Alignment.Center
//    ) {
//      Text("No student applications available")
//    }
//    return
//  }
//
//  Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
//    FlowRow(
//      horizontalArrangement = Arrangement.spacedBy(8.dp),
//      verticalArrangement = Arrangement.spacedBy(8.dp),
//    ) {
//      uiState.studentApplications.forEach { application ->
//        StudentApplicationItem(application)
//      }
//    }
//  }
}
//
// val data = StudentApplicationsQuery.StudentsApplied(
//  id = "fdfdfdf",
//  studentName = "आर्य प्रितम",
//  studentPhoto = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/documents//123312312123_student_photo_1741460570.jpg",
//  previousClass = "7th",
//  marksObtained = "73 %"
// )
//
// @Preview
// @Composable
// fun StudentApplicationItemPreview() {
//  StudentApplicationItem(data)
// }
//
// @Composable
// fun StudentApplicationItem(data: StudentApplicationsQuery.StudentsApplied) {
//  ElevatedCard(
//    modifier = Modifier.width(300.dp),
//    shape = RoundedCornerShape(4.dp),
//    onClick = {}
//  ){
//    Box(modifier = Modifier.padding(8.dp)) {
//      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//        AsyncImage(
//          modifier = Modifier.size(96.dp),
//          model = data.studentPhoto,
//          contentDescription = data.studentName,
//        )
//        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
//          Text(
//            text = data.studentName,
//            style = MaterialTheme.typography.bodyLarge,
//            fontWeight = FontWeight.Bold,
//          )
//          Text(
//            text = "पिछली उत्तीर्ण कक्षा: ${data.previousClass}",
//            style = MaterialTheme.typography.bodyMedium
//          )
//          Text(
//            text = "प्राप्त अंक: ${data.marksObtained}",
//            style = MaterialTheme.typography.bodyMedium
//          )
//        }
//      }
//    }
//  }
// }
