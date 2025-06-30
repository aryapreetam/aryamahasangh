package com.aryamahasangh

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Video(
  val id: String = "",
  val title: String,
  val description: String,
  val url: String,
  @SerialName("thumbnail_url")
  val thumbnailUrl: String,
  @SerialName("video_id")
  val videoId: String
)