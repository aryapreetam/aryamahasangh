package com.aryamahasangh.features.admin

import kotlinx.datetime.format
import kotlinx.datetime.format.char
import com.aryamahasangh.components.AddressData
import com.aryamahasangh.components.FamilyRelation as ComponentsFamilyRelation
import com.aryamahasangh.type.FamilyRelation as GraphQLFamilyRelation

// Data classes for family display
data class FamilyShort(
  val id: String,
  val name: String,
  val photos: List<String> = emptyList(),
  val address: String = "",
  val aryaSamajName: String = ""
)

// Detailed family data for family detail screen
data class FamilyDetail(
  val id: String,
  val name: String,
  val photos: List<String> = emptyList(),
  val address: FamilyAddress? = null,
  val aryaSamaj: FamilyAryaSamaj? = null,
  val members: List<FamilyMember> = emptyList()
)

data class FamilyAddress(
  val id: String,
  val basicAddress: String,
  val state: String,
  val district: String,
  val pincode: String,
  val latitude: Double? = null,
  val longitude: Double? = null,
  val vidhansabha: String = ""
)

data class FamilyAryaSamaj(
  val id: String,
  val name: String,
  val address: String = ""
)

data class FamilyMember(
  val id: String,
  val name: String,
  val profileImage: String? = null,
  val phoneNumber: String? = null,
  val joiningDate: kotlinx.datetime.LocalDate? = null,
  val isHead: Boolean = false,
  val relationToHead: ComponentsFamilyRelation? = null
)

// Address with associated member IDs
data class AddressWithMemberId(
  val addressId: String,
  val memberIds: List<String>, // Members who have this address
  val addressData: AddressData
)

// Extension functions to convert between component and GraphQL FamilyRelation types
fun ComponentsFamilyRelation.toGraphQL(): GraphQLFamilyRelation =
  when (this) {
    ComponentsFamilyRelation.SELF -> GraphQLFamilyRelation.SELF
    ComponentsFamilyRelation.FATHER -> GraphQLFamilyRelation.FATHER
    ComponentsFamilyRelation.MOTHER -> GraphQLFamilyRelation.MOTHER
    ComponentsFamilyRelation.HUSBAND -> GraphQLFamilyRelation.HUSBAND
    ComponentsFamilyRelation.WIFE -> GraphQLFamilyRelation.WIFE
    ComponentsFamilyRelation.SON -> GraphQLFamilyRelation.SON
    ComponentsFamilyRelation.DAUGHTER -> GraphQLFamilyRelation.DAUGHTER
    ComponentsFamilyRelation.BROTHER -> GraphQLFamilyRelation.BROTHER
    ComponentsFamilyRelation.SISTER -> GraphQLFamilyRelation.SISTER
    ComponentsFamilyRelation.GRANDFATHER -> GraphQLFamilyRelation.GRANDFATHER
    ComponentsFamilyRelation.GRANDMOTHER -> GraphQLFamilyRelation.GRANDMOTHER
    ComponentsFamilyRelation.GRANDSON -> GraphQLFamilyRelation.GRANDSON
    ComponentsFamilyRelation.GRANDDAUGHTER -> GraphQLFamilyRelation.GRANDDAUGHTER
    ComponentsFamilyRelation.UNCLE -> GraphQLFamilyRelation.UNCLE
    ComponentsFamilyRelation.AUNT -> GraphQLFamilyRelation.AUNT
    ComponentsFamilyRelation.COUSIN -> GraphQLFamilyRelation.COUSIN
    ComponentsFamilyRelation.NEPHEW -> GraphQLFamilyRelation.NEPHEW
    ComponentsFamilyRelation.NIECE -> GraphQLFamilyRelation.NIECE
    ComponentsFamilyRelation.GUARDIAN -> GraphQLFamilyRelation.GUARDIAN
    ComponentsFamilyRelation.RELATIVE -> GraphQLFamilyRelation.RELATIVE
    ComponentsFamilyRelation.OTHER -> GraphQLFamilyRelation.OTHER
  }

fun GraphQLFamilyRelation.toComponents(): ComponentsFamilyRelation =
  when (this) {
    GraphQLFamilyRelation.SELF -> ComponentsFamilyRelation.SELF
    GraphQLFamilyRelation.FATHER -> ComponentsFamilyRelation.FATHER
    GraphQLFamilyRelation.MOTHER -> ComponentsFamilyRelation.MOTHER
    GraphQLFamilyRelation.HUSBAND -> ComponentsFamilyRelation.HUSBAND
    GraphQLFamilyRelation.WIFE -> ComponentsFamilyRelation.WIFE
    GraphQLFamilyRelation.SON -> ComponentsFamilyRelation.SON
    GraphQLFamilyRelation.DAUGHTER -> ComponentsFamilyRelation.DAUGHTER
    GraphQLFamilyRelation.BROTHER -> ComponentsFamilyRelation.BROTHER
    GraphQLFamilyRelation.SISTER -> ComponentsFamilyRelation.SISTER
    GraphQLFamilyRelation.GRANDFATHER -> ComponentsFamilyRelation.GRANDFATHER
    GraphQLFamilyRelation.GRANDMOTHER -> ComponentsFamilyRelation.GRANDMOTHER
    GraphQLFamilyRelation.GRANDSON -> ComponentsFamilyRelation.GRANDSON
    GraphQLFamilyRelation.GRANDDAUGHTER -> ComponentsFamilyRelation.GRANDDAUGHTER
    GraphQLFamilyRelation.UNCLE -> ComponentsFamilyRelation.UNCLE
    GraphQLFamilyRelation.AUNT -> ComponentsFamilyRelation.AUNT
    GraphQLFamilyRelation.COUSIN -> ComponentsFamilyRelation.COUSIN
    GraphQLFamilyRelation.NEPHEW -> ComponentsFamilyRelation.NEPHEW
    GraphQLFamilyRelation.NIECE -> ComponentsFamilyRelation.NIECE
    GraphQLFamilyRelation.GUARDIAN -> ComponentsFamilyRelation.GUARDIAN
    GraphQLFamilyRelation.RELATIVE -> ComponentsFamilyRelation.RELATIVE
    GraphQLFamilyRelation.OTHER -> ComponentsFamilyRelation.OTHER
    GraphQLFamilyRelation.UNKNOWN__ -> ComponentsFamilyRelation.OTHER
  }

// Extension function to format joining date in Hindi
fun kotlinx.datetime.LocalDate.toHindiDateString(): String {
  val formatter =
    kotlinx.datetime.LocalDate.Format {
      dayOfMonth()
      char('/')
      monthNumber()
      char('/')
      year()
    }
  return format(formatter)
}
