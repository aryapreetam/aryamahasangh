package org.aryamahasangh.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.aryamahasangh.repository.LearningItem
import org.aryamahasangh.repository.LearningRepository
import org.aryamahasangh.util.Result

/**
 * UI state for the Learning screen
 */
data class LearningUiState(
  val learningItems: List<LearningItem> = emptyList(),
  val isLoading: Boolean = false,
  val error: String? = null
)

/**
 * UI state for the Video Player screen
 */
data class VideoPlayerUiState(
  val learningItem: LearningItem? = null,
  val isLoading: Boolean = false,
  val error: String? = null
)

/**
 * ViewModel for the Learning and Video Player screens
 */
class LearningViewModel(
  private val learningRepository: LearningRepository
) : BaseViewModel<LearningUiState>(LearningUiState()) {

  // Separate state for video player
  private val _videoPlayerUiState = MutableStateFlow(VideoPlayerUiState())
  val videoPlayerUiState: StateFlow<VideoPlayerUiState> = _videoPlayerUiState.asStateFlow()

  init {
    loadLearningItems()
  }

  /**
   * Load learning items from the repository
   */
  fun loadLearningItems() {
    launch {
      learningRepository.getLearningItems().collect { result ->
        when (result) {
          is Result.Loading -> {
            updateState { it.copy(isLoading = true, error = null) }
          }
          is Result.Success -> {
            updateState { it.copy(
              learningItems = result.data,
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

  /**
   * Load learning item details by ID
   */
  fun loadLearningItemDetail(id: String) {
    launch {
      _videoPlayerUiState.value = VideoPlayerUiState(isLoading = true)

      when (val result = learningRepository.getLearningItemDetail(id)) {
        is Result.Success -> {
          _videoPlayerUiState.value = VideoPlayerUiState(
            learningItem = result.data,
            isLoading = false,
            error = null
          )
        }
        is Result.Error -> {
          _videoPlayerUiState.value = VideoPlayerUiState(
            isLoading = false,
            error = result.message
          )
        }
        is Result.Loading -> {
          // This shouldn't happen with the current implementation
        }
      }
    }
  }
}
