package com.aryamahasangh.features.gurukul.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aryamahasangh.components.EventListItem
import com.aryamahasangh.features.arya_nirman.UpcomingActivity
import com.aryamahasangh.features.gurukul.presenter.CourseItemUiModel
import com.aryamahasangh.features.gurukul.presenter.UpcomingCoursesUiState
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Pure render Composable for upcoming courses screen. Stateless.
 * Now consumes the current UiState (not StateFlow), and emits navigation upward.
 * Gold standard for testability and separation.
 */
@Composable
fun UpcomingCoursesScreen(
    uiState: UpcomingCoursesUiState,
    onNavigateToRegister: (courseId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    when(uiState) {
        is UpcomingCoursesUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize().testTag("loading_state")) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
        is UpcomingCoursesUiState.Error -> {
            val errorMsg = (uiState as UpcomingCoursesUiState.Error).message
            Box(modifier = modifier.fillMaxSize().testTag("error_state").semantics { contentDescription = "error" }) {
                Text(errorMsg)
            }
        }
        is UpcomingCoursesUiState.Success -> {
            val courses = (uiState as UpcomingCoursesUiState.Success).courses
            if (courses.isEmpty()) {
                // Empty state for no courses
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .testTag("empty_state")
                        .semantics { contentDescription = "empty courses" },
                    contentAlignment = Alignment.Center
                ) {
                    Text("कोई कक्षा नियोजित नहीं है!", style = MaterialTheme.typography.titleMedium)
                }
            } else {
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                courses.forEach { course ->
                  val event = course.toUpcomingActivityWithInstantParse()
                  EventListItem(
                    event = event,
                    onRegisterClick = { onNavigateToRegister(course.id) },
                    onDirectionsClick = null, // implement if needed
                    modifier = Modifier
                      .testTag("course_card_${course.id}")
                      .semantics { contentDescription = "course card ${course.name}" }
                  )
                }
              }
            }
        }
        is UpcomingCoursesUiState.NavigateToRegistration -> { /* handled by effect/hook, not UI here */ }
    }
}

/**
 * Extension: maps every field in CourseItemUiModel to UpcomingActivity, so EventListItem renders complete info.
 * Converts Instant ISO string ('2025-09-30T02:30:00Z') to LocalDateTime in default time zone if possible.
 */
fun CourseItemUiModel.toUpcomingActivityWithInstantParse(): UpcomingActivity {
    fun parseToLocalDateTime(str: String): kotlinx.datetime.LocalDateTime {
        return try {
            val instant = Instant.parse(str)
            instant.toLocalDateTime(TimeZone.currentSystemDefault())
        } catch (e: Exception) {
            // Fallback: try parse as LocalDateTime or use some default
            kotlinx.datetime.LocalDateTime(1970, 1, 1, 0, 0)
        }
    }
    return UpcomingActivity(
        id = id,
        name = name,
        genderAllowed = com.aryamahasangh.features.activities.GenderAllowed.ANY, 
        isFull = !panjikaranOpen,
        startDateTime = parseToLocalDateTime(startDatetime),
        endDateTime = parseToLocalDateTime(endDatetime),
        district = district,
        state = state,
        latitude = latitude,
        longitude = longitude,
        capacity = capacity
    )
}
