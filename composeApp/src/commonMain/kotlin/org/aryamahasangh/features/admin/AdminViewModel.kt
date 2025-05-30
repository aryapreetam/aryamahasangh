package org.aryamahasangh.features.admin

import org.aryamahasangh.viewmodel.BaseViewModel

data class MembersUiState(
  val data: List<MemberShort> = emptyList(),
  val isLoading: Boolean = false,
  val error: String? = null,
)

class AdminViewModel(private val repository: AdminRepository) : BaseViewModel<MembersUiState>(MembersUiState()) {

}
