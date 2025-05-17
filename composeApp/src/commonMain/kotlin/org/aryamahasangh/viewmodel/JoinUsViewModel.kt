package org.aryamahasangh.viewmodel

import org.aryamahasangh.OrganisationalActivitiesQuery
import org.aryamahasangh.repository.JoinUsRepository
import org.aryamahasangh.type.ActivityFilterInput
import org.aryamahasangh.util.Result

/**
 * UI state for the Join Us screen
 */
data class JoinUsUiState(
  val activities: List<OrganisationalActivitiesQuery.OrganisationalActivity>? = null,
  val isLoading: Boolean = false,
  val error: String? = null,
  val labelState: LabelState = LabelState()
)

data class LabelState(
  val label: String = "Join Us",
  val isUpdating: Boolean = false,
  val error: String? = null,
  val editMode: Boolean = false
)

/**
 * ViewModel for the Join Us screen
 */
class JoinUsViewModel(
  private val joinUsRepository: JoinUsRepository
) : BaseViewModel<JoinUsUiState>(JoinUsUiState()) {


  // Add this function to your ViewModel
  fun setEditMode(enabled: Boolean) {
    updateState { it.copy(labelState = it.labelState.copy(editMode = enabled)) }
  }

  fun loadJoinUsLabel(){
    launch {

      joinUsRepository.getJoinUsLabel().collect { result ->
        when (result) {
          is Result.Loading -> {
            //updateState { it.copy(isLoading = true, error = null) }
            println("loading label ")
          }
          is Result.Success -> {
            updateState { it.copy(
              labelState = LabelState(label = result.data, isUpdating = false, error = null, editMode = false),
              isLoading = false,
              error = null
            )}
            println("label loaded successfully: ${result.data}")
          }
          is Result.Error -> {
            println("error loading label")
          }
        }
      }
    }
  }

  fun updateJoinUsLabel(label: String) {
    launch {
      updateState { it.copy(labelState = it.labelState.copy(isUpdating = true, editMode = true)) }
      joinUsRepository.updateLabel(label).collect { result ->
        when(result){
          is Result.Error -> {
            updateState { it.copy(labelState = it.labelState.copy(isUpdating = false, error = result.message, editMode = true)) }
            println("error updating label: ${result.message}")
          }
          is Result.Loading -> {
            updateState { it.copy(labelState = it.labelState.copy(isUpdating = true)) }
          }
          is Result.Success<*> -> {
            updateState { it.copy(labelState = it.labelState.copy(isUpdating = false, editMode = false)) }
            loadJoinUsLabel()
            println("label updated successfully: ${result.data}")
          }
        }
      }
    }
  }

  /**
   * Load filtered activities
   */
  fun loadFilteredActivities(filter: ActivityFilterInput) {
    launch {
      updateState { it.copy(isLoading = true, error = null) }
      
      joinUsRepository.getFilteredActivities(filter).collect { result ->
        when (result) {
          is Result.Loading -> {
            updateState { it.copy(isLoading = true, error = null) }
          }
          is Result.Success -> {
            updateState { it.copy(
              activities = result.data,
              isLoading = false,
              error = null
            )}
          }
          is Result.Error -> {
            updateState { it.copy(
              isLoading = false,
              error = result.message
            )}
          }
        }
      }
    }
  }
}