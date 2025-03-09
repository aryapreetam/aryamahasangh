package org.aryamahasangh.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.aryamahasangh.StudentApplicationsQuery
import org.aryamahasangh.network.apolloClient
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReceivedApplicationsScreen(){
  var receivedApplications by remember { mutableStateOf(emptyList<StudentApplicationsQuery.StudentsApplied>()) }
  LaunchedEffect(Unit){
    val res = apolloClient.query(StudentApplicationsQuery()).execute()
    receivedApplications = res.data?.studentsApplied ?: emptyList()
  }
  Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
    FlowRow {
      receivedApplications.forEach {
        StudentApplicationItem(it)
      }
    }
  }
}

val data = StudentApplicationsQuery.StudentsApplied(
  id = "fdfdfdf",
  studentName = "आर्य प्रितम",
  studentPhoto = "https://placeholder-staging-supabase.co/storage/v1/object/public/documents//123312312123_student_photo_1741460570.jpg",
  previousClass = "7th",
  marksObtained = "73 %"
)

@Preview
@Composable
fun StudentApplicationItemPreview() {
  StudentApplicationItem(data)
}

@Composable
fun StudentApplicationItem(data: StudentApplicationsQuery.StudentsApplied) {
  ElevatedCard(
    modifier = Modifier.width(300.dp),
    shape = RoundedCornerShape(4.dp),
    onClick = {}
  ){
    Column(modifier = Modifier.padding(8.dp)) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AsyncImage(
          modifier = Modifier.size(96.dp),
          model = data.studentPhoto,
          contentDescription = data.studentName,
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = data.studentName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "पिछली उत्तीर्ण कक्षा: ${data.previousClass}",
            style = MaterialTheme.typography.bodyMedium
          )
          Text(
            text = "प्राप्त अंक: ${data.marksObtained}",
            style = MaterialTheme.typography.bodyMedium
          )
        }
      }
    }
  }
}