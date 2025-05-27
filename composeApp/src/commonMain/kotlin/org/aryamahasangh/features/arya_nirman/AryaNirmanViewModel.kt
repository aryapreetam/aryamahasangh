package org.aryamahasangh.features.arya_nirman

import org.aryamahasangh.viewmodel.BaseViewModel

data class UiState(val isLoading: Boolean = false)

class AryaNirmanViewModel(
  private val aryaNirmanRepository: AryaNirmanRepository
) : BaseViewModel<UiState>(UiState()) {

}