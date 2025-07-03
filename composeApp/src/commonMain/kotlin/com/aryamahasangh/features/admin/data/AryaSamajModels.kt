package com.aryamahasangh.features.admin.data

import com.aryamahasangh.components.AddressData
import com.aryamahasangh.components.ImagePickerState
import com.aryamahasangh.components.MembersState
import com.aryamahasangh.fragment.AryaSamajWithAddress

/**
 * Arya Samaj list item for display in list
 */
data class AryaSamajListItem(
  val id: String,
  val name: String,
  val description: String,
  val formattedAddress: String,
  val memberCount: Int = 0,
  val mediaUrls: List<String> = emptyList()
)

/**
 * Detailed Arya Samaj information
 */
data class AryaSamajDetail(
  val id: String,
  val name: String,
  val description: String,
  val mediaUrls: List<String>,
  val address: AddressData,
  val members: List<AryaSamajMember>,
  val createdAt: String? = null
)

/**
 * Arya Samaj member with role information
 */
data class AryaSamajMember(
  val id: String,
  val memberId: String,
  val memberName: String,
  val memberProfileImage: String?,
  val memberPhoneNumber: String?,
  val post: String,
  val priority: Int
)

/**
 * Form data for creating/editing Arya Samaj
 */
data class AryaSamajFormData(
  val name: String = "",
  val description: String = "",
  val imagePickerState: ImagePickerState = ImagePickerState(),
  val addressData: AddressData = AddressData(),
  val membersState: MembersState = MembersState()
) {
  fun hasUnsavedChanges(initialData: AryaSamajFormData): Boolean {
    return this != initialData
  }
}

// Extension functions for generated AryaSamajWithAddress type
fun AryaSamajWithAddress.getFormattedAddress(): String {
  return address?.addressFields?.let { addr ->
    val parts = listOfNotNull(
      addr.basicAddress?.takeIf { it.isNotBlank() },
      addr.district?.takeIf { it.isNotBlank() },
      addr.state?.takeIf { it.isNotBlank() }
    )
    parts.joinToString(", ")
  } ?: ""
}

// Optional: Extension to get district/state separately if needed
fun AryaSamajWithAddress.getDistrict(): String = address?.addressFields?.district ?: ""
fun AryaSamajWithAddress.getState(): String = address?.addressFields?.state ?: ""
