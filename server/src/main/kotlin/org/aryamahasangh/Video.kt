package org.aryamahasangh

import com.expediagroup.graphql.generator.scalars.ID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class Video(val id: ID, val title: String, val description: String, val url: String, val thumbnailUrl: String, val videoId: String)

@OptIn(ExperimentalUuidApi::class)
val videosList: List<Video> = buildList {
  repeat(11){
    add(
      Video(
        id = ID(Uuid.random().toString()),
        title = "सत्यार्थ प्रकाश व्याख्या भाग १",
        description = "ईश्वर के नाम की व्याख्या भाग 1",
        url = "https://www.youtube.com/watch?v=VRV-DbnKekM",
        thumbnailUrl = "https://ftnwwiwmljcwzpsawdmf.supabase.co/storage/v1/object/public/images//satyarth_prakash_video_1.webp",
        videoId = "VRV-DbnKekM"
      )
    )
  }
}