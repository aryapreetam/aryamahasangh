package org.aryamahasangh

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable // Annotation for kotlinx.serialization library
data class BookOrder(
  @SerialName("id")
  val id: String,

  @SerialName("fullname")
  val fullname: String,

  @SerialName("address")
  val address: String,

  @SerialName("city")
  val city: String,

  @SerialName("district")
  val district: String,

  @SerialName("state")
  val state: String,

  @SerialName("mobile")
  val mobile: String,

  @SerialName("pincode")
  val pincode: String,

  @SerialName("country")
  val country: String,

  @SerialName("district_officer_name")
  val districtOfficerName: String,

  @SerialName("district_officer_number")
  val districtOfficerNumber: String,

  @SerialName("payment_receipt_url")
  val paymentReceiptUrl: String,

  @SerialName("is_fulfilled") // Note: 'fullfilled' might be a typo for 'fulfilled'
  val isFulfilled: Boolean,

  @SerialName("created_at")
  val createdAt: String // This is a timestamp string, you might want to parse it later
)

@Serializable // Annotation for kotlinx.serialization library
data class BookOrderInput(

  @SerialName("fullname")
  val fullname: String,

  @SerialName("address")
  val address: String,

  @SerialName("city")
  val city: String,

  @SerialName("district")
  val district: String,

  @SerialName("state")
  val state: String,

  @SerialName("mobile")
  val mobile: String,

  @SerialName("pincode")
  val pincode: String,

  @SerialName("country")
  val country: String,

  @SerialName("district_officer_name")
  val districtOfficerName: String,

  @SerialName("district_officer_number")
  val districtOfficerNumber: String,

  @SerialName("payment_receipt_url")
  val paymentReceiptUrl: String,
)