package com.aryamahasangh.features.gurukul.viewmodel

import com.aryamahasangh.features.gurukul.data.GurukulRepository
import com.aryamahasangh.type.GenderFilter
import com.aryamahasangh.utils.formatShortForBook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.aryamahasangh.features.gurukul.ui.CourseDropdownItem
import com.aryamahasangh.features.gurukul.ui.CourseRegistrationReceivedItem
import com.aryamahasangh.features.gurukul.ui.CourseRegistrationsReceivedUiState

class CourseRegistrationsReceivedViewModel(
  private val gurukulRepository: GurukulRepository,
  private val genderFilter: GenderFilter
) {
  private val _uiState = MutableStateFlow(CourseRegistrationsReceivedUiState(isLoading = true))
  val uiState: StateFlow<CourseRegistrationsReceivedUiState> = _uiState

  init {
    loadCourses()
  }

  private fun loadCourses() {
    CoroutineScope(Dispatchers.Main).launch {
      _uiState.value = _uiState.value.copy(isLoading = true, isError = false)
      try {
        val courses = gurukulRepository.getCourses(genderFilter)
        val dropdownItems = courses.map { course ->
          CourseDropdownItem(
            id = course.id,
            name = course.name,
            shortDescription = course.shortDescription,
            startDate = formatShortForBook(course.startDatetime.toString()),
            endDate = formatShortForBook(course.endDatetime.toString()),
            place = listOfNotNull(course.address?.district, course.address?.state)
              .filter { it.isNotBlank() }
              .joinToString(", ")
          )
        }
        _uiState.value = _uiState.value.copy(isLoading = false, isError = false, courses = dropdownItems)
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(isLoading = false, isError = true, errorMessage = e.message)
      }
    }
  }

  fun loadRegistrations(courseId: String) {
    CoroutineScope(Dispatchers.Main).launch {
      _uiState.value = _uiState.value.copy(isLoading = true)
      try {
        val result = gurukulRepository.getCourseRegistrationsForActivity(courseId)
        if (result.isSuccess) {
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            registrations = result.getOrThrow(),
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
}
