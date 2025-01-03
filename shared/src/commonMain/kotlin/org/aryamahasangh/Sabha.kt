package org.aryamahasangh

data class Sabha(
  val name: String,
  val logo: String,
  val description: String,
  val keyPeople: List<OrganisationalMember> = listOf(),
  val campaigns: List<Campaign> = listOf(),
  val sessions: List<Session> = listOf(),
  val events: List<Event> = listOf(),
)

data class OrganisationalMember(
  val member: Member,
  val post: String,
  val priority: Int
)

data class Member(
  val name: String,
  val educationalQualification: String?,
  val profileImage: String?,
)

/**
 * Karyakram:
 */
data class Event(val name: String)

/**
 * Satr:
 */
data class Session(val name: String)

/**
 * Abhiyan:
 */
data class Campaign(val name: String)

val listOfSabha = listOf(
  "राष्ट्रीय आर्य निर्मात्री सभा",
  "राष्ट्रीय आर्य क्षत्रिय सभा",
  "राष्ट्रीय आर्य संरक्षिणी सभा",
  "राष्ट्रीय आर्य संवर्धिनी सभा",
  "राष्ट्रीय आर्य दलितोद्धारिणी सभा",
  "आर्य गुरुकुल महाविद्यालय",
  "आर्या गुरुकुल महाविद्यालय",
  "आर्या परिषद्",
  "वानप्रस्थ आयोग",
  "राष्ट्रीय आर्य छात्र सभा",
  "राष्ट्रीय आर्य संचार परिषद",
  "केंद्रिय वित्तीय प्रबंधन परिषद",
)