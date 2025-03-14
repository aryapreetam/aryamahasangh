package org.aryamahasangh

import kotlinx.serialization.Serializable

@Serializable
data class AdmissionFormData(
  val id: String,
  val studentName: String = "",
  val aadharNo: String = "",
  val dob: String = "",
  val bloodGroup: String = "",
  val previousClass: String = "",
  val marksObtained: String = "",
  val schoolName: String = "",
  val fatherName: String = "",
  val fatherOccupation: String = "",
  val fatherQualification: String = "",
  val motherName: String = "",
  val motherOccupation: String = "",
  val motherQualification: String = "",
  val fullAddress: String = "",
  val mobileNo: String = "",
  val alternateMobileNo: String = "",
  val attachedDocuments: List<String> = emptyList(),
  val studentPhoto: String,
  val studentSignature: String,
  val parentSignature: String,
)