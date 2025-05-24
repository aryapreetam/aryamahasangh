package org.aryamahasangh.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.aryamahasangh.BookOrderQuery
import org.aryamahasangh.BookOrdersQuery
import org.aryamahasangh.CreateBookOrderMutation
import org.aryamahasangh.repository.BookOrderRepository
import org.aryamahasangh.type.BookOrderInput
import org.aryamahasangh.util.Result

sealed class BookOrderUiState protected constructor(open val isLoading: Boolean = false, open val error: String? = null)

data class DefaultBookOrderUiState(
  override val isLoading: Boolean = false,
  override val error: String? = null
) : BookOrderUiState(isLoading, error)

data class CreateBookOrderUiState(
  override val isLoading: Boolean = false,
  val createdBookOrder: CreateBookOrderMutation.CreateBookOrder?,
  override val error: String? = null
) : BookOrderUiState(isLoading, error)

data class BookOrdersUiState(
  override val isLoading: Boolean = false,
  val bookOrders: List<BookOrdersQuery.BookOrder>,
  override val error: String? = null
) : BookOrderUiState(isLoading, error)

data class BookOrderDetailsUiState(
  override val isLoading: Boolean = false,
  val bookOrder: BookOrderQuery.BookOrder?,
  override val error: String? = null
) : BookOrderUiState(isLoading, error)

class BookOrderViewModel(private val bookOrderRepository: BookOrderRepository) :
  BaseViewModel<BookOrderUiState>(DefaultBookOrderUiState()) {
  private val _createBookOrderState = MutableStateFlow<CreateBookOrderUiState>(CreateBookOrderUiState(
    isLoading = false,
    createdBookOrder = null,
    error = null
  ))
  val createBookOrderState: StateFlow<CreateBookOrderUiState?> = _createBookOrderState.asStateFlow()

  private val _bookOrdersState = MutableStateFlow<BookOrdersUiState>(BookOrdersUiState(
    bookOrders = emptyList(),
    isLoading = false,
    error = null
  ))
  val bookOrdersState: StateFlow<BookOrdersUiState> = _bookOrdersState.asStateFlow()

  private val _bookOrderDetailsState = MutableStateFlow<BookOrderDetailsUiState>(BookOrderDetailsUiState(
    isLoading = false,
    bookOrder = null,
    error = null
  ))
  val bookOrderDetailsState: StateFlow<BookOrderDetailsUiState> = _bookOrderDetailsState.asStateFlow()

  init {
    loadBookOrders()
  }

  fun updateBookOrdersState(result: Result<List<BookOrdersQuery.BookOrder>>) {
    when (result) {
      is Result.Error -> _bookOrdersState.update {
        it.copy(
          error = result.message,
          isLoading = false
        )
      }

      is Result.Loading -> _bookOrdersState.update {
        it.copy(
          isLoading = true,
          error = null
        )
      }

      is Result.Success -> _bookOrdersState.update {
        it.copy(
          bookOrders = result.data,
          isLoading = false,
          error = null
        )
      }
    }
  }

  fun loadBookOrders() {
    println("BookOrderViewModel: Loading book orders")
    launch {
      bookOrderRepository.getBookOrders().collect { result ->
        println("BookOrderViewModel: Received result: $result")
        updateBookOrdersState(result)
        println("BookOrderViewModel: Updated state: ${_bookOrdersState.value}")
      }
    }
  }

  fun createBookOrder(input: BookOrderInput) {
    launch {

      bookOrderRepository.addBookOrder(input).collect { result ->
        when (result) {
          is Result.Success -> _createBookOrderState.value =
            _createBookOrderState.value.copy(
              createdBookOrder = result.data,
              isLoading = false,
              error = null
            )

          is Result.Error -> _createBookOrderState.value = _createBookOrderState.value.copy(
            error = result.message,
            createdBookOrder = null,
            isLoading = false,
          )

          is Result.Loading -> _createBookOrderState.value = _createBookOrderState.value.copy(
            isLoading = true,
            createdBookOrder = null,
            error = null
          )
        }
      }
    }
  }

  fun getBookOrderById(id: String) {
    launch {
      bookOrderRepository.getBookOrderById(id).collect { result ->
        when (result) {
          is Result.Success -> _bookOrderDetailsState.value =
            _bookOrderDetailsState.value.copy(
              bookOrder = result.data,
              error = null,
              isLoading = false
            )
          is Result.Error -> _bookOrderDetailsState.value = _bookOrderDetailsState.value.copy(
            error = result.message,
            isLoading = false,
            bookOrder = null
          )

          is Result.Loading -> _bookOrderDetailsState.value = _bookOrderDetailsState.value.copy(
            isLoading = true,
            bookOrder = null,
            error = null
          )
        }
      }
    }
  }
}
