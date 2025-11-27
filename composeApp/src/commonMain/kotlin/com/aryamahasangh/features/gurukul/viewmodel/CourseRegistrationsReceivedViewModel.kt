package com.aryamahasangh.features.gurukul.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aryamahasangh.features.arya_nirman.convertDates
import com.aryamahasangh.features.gurukul.data.GurukulRepository
import com.aryamahasangh.features.gurukul.domain.models.Course
import com.aryamahasangh.type.GenderFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class CourseRegistrationReceivedItem(
  val id: String,
  val name: String,
  val date: String,
  val place: String,
  val recommendation: String,
  val receiptUrl: String?,
  val photoUrl: String?,
  val dob: String,
  val phoneNumber: String,
  val qualification: String,
  val guardianName: String,
  val address: String
)

data class CourseDropdownItem(
  val id: String,
  val name: String,
  val shortDescription: String,
  val formattedDateRange: String,
  val formattedTimeRange: String,
  val place: String
)

data class CourseRegistrationsReceivedUiState(
  val isLoading: Boolean = false,
  val isError: Boolean = false,
  val courses: List<CourseDropdownItem> = emptyList(),
  val selectedCourseId: String? = null,
  val registrations: List<CourseRegistrationReceivedItem> = emptyList(),
  val errorMessage: String? = null,
  val isDropdownExpanded: Boolean = false
)

class CourseRegistrationsReceivedViewModel(
  private val gurukulRepository: GurukulRepository,
  private val genderFilter: GenderFilter
): ViewModel() {
  private val _uiState = MutableStateFlow(CourseRegistrationsReceivedUiState(isLoading = true))
  val uiState: StateFlow<CourseRegistrationsReceivedUiState> = _uiState

  init {
    loadCourses()
  }

  private fun loadCourses() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, isError = false)
      try {
        // Use getAllCourses to fetch all courses (past, present, future) sorted by date descending
        val courses = gurukulRepository.getAllCourses(genderFilter)
        val dropdownItems = courses.map { course ->
          mapCourseDropdown(course)
        }
        _uiState.value = _uiState.value.copy(isLoading = false, isError = false, courses = dropdownItems)
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(isLoading = false, isError = true, errorMessage = e.message)
      }
    }
  }

  fun loadRegistrations(courseId: String) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)
      try {
        val result = gurukulRepository.getCourseRegistrationsForActivity(courseId)
        if (result.isSuccess) {
          val registrations = result.getOrThrow().map { registration ->
            mapRegistration(registration)
          }
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            registrations = registrations,
            isError = false,
            errorMessage = null
          )
        } else {
          _uiState.value = _uiState.value.copy(isLoading = false, isError = true, errorMessage = "त्रुटि")
        }
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(isLoading = false, isError = true, errorMessage = e.message)
      }
    }
  }

  fun onDropdownExpandChanged(expanded: Boolean) {
    _uiState.value = _uiState.value.copy(isDropdownExpanded = expanded)
  }

  fun onCourseSelected(courseId: String) {
    _uiState.value = _uiState.value.copy(selectedCourseId = courseId)
    loadRegistrations(courseId)
  }

  // Click handling, could open link (for now a stub)
  fun onReceiptClicked(url: String) {
    println("Receipt clicked: $url")
  }

  fun mapRegistration(
    raw: com.aryamahasangh.CourseRegistrationsForActivityQuery.Node
  ): CourseRegistrationReceivedItem {
    return CourseRegistrationReceivedItem(
      id = raw.id.toString(),
      name = raw.name ?: "",
      date = raw.satrDate.toString(),
      place = raw.satrPlace ?: "",
      recommendation = raw.recommendation ?: "",
      receiptUrl = raw.paymentReceiptUrl,
      photoUrl = raw.photoUrl,
      dob = raw.dob?.toString() ?: "",
      phoneNumber = raw.phoneNumber ?: "",
      qualification = raw.qualification ?: "",
      guardianName = raw.guardianName ?: "",
      address = raw.address ?: ""
    )
  }

  fun mapCourseDropdown(
    raw: Course
  ): CourseDropdownItem {
    val (dateRange, timeRange) = convertDates(
      raw.startDatetime.toLocalDateTime(TimeZone.currentSystemDefault()),
      raw.endDatetime.toLocalDateTime(TimeZone.currentSystemDefault())
    )
    return CourseDropdownItem(
      id = raw.id,
      name = raw.name,
      shortDescription = raw.shortDescription,
      formattedDateRange = dateRange,
      formattedTimeRange = timeRange,
      place = listOfNotNull(raw.address?.district, raw.address?.state)
        .filter { it.isNotBlank() }
        .joinToString(", ")
    )
  }
}
