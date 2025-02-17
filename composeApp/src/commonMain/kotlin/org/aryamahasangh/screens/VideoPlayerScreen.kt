package org.aryamahasangh.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.aryamahasangh.LearningItemDetailQuery
import org.aryamahasangh.YoutubeVideoPlayer
import org.aryamahasangh.network.apolloClient

@Composable
fun VideoDetailsScreen(learningItemId: String) {
  val learningItemDetail = remember { mutableStateOf<LearningItemDetailQuery.LearningItem?>(null) }
  LaunchedEffect(Unit) {
    val res = apolloClient.query(LearningItemDetailQuery(learningItemId)).execute()
    learningItemDetail.value = res.data?.learningItem
  }
  if(learningItemDetail.value == null){
    return
  }
  val detail = learningItemDetail.value!!
  Column(
    modifier =  Modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    YoutubeVideoPlayer(detail.videoId)
    Text(detail.title,
      style =  MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
    )
//    Text(detail.description)
    Text("ईश्वर के नाम की व्याख्या भाग 1: " +
        "प्रस्तुत श्रव्यकणिका में आचार्य जी ने सत्यार्थ प्रकाश के प्रथम समुल्लास में वर्णित ईश्वर के नामों, जैसे वरुण, अर्यमा, इंद्र, वृहस्पति इत्यादि की व्याख्या अत्यंत सरल शब्दों में की है। \n" +
        "वैसे तो प्रथम समुल्लास सत्यार्थ प्रकाश में सबसे कठिन माना जाता है पर आचार्य जी ने इसे विविध उदहारण देकर रुचिपूर्वक बनाया है।")
  }
}