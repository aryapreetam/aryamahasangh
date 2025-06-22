package org.aryamahasangh.features.admin.data

import org.aryamahasangh.components.AddressData
import org.aryamahasangh.components.ImagePickerState
import org.aryamahasangh.components.MembersState

/**
 * Arya Samaj list item for display in list
 */
data class AryaSamajListItem(
  val id: String,
  val name: String,
  val description: String,
  val formattedAddress: String,
  val memberCount: Int = 0
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
