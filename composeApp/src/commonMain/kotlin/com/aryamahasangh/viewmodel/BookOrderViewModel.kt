package com.aryamahasangh.viewmodel

import com.aryamahasangh.repository.BookOrderRepository

sealed class BookOrderUiState protected constructor(open val isLoading: Boolean = false, open val error: String? = null)

data class DefaultBookOrderUiState(
  override val isLoading: Boolean = false,
  override val error: String? = null
) : BookOrderUiState(isLoading, error)

// data class CreateBookOrderUiState(
//  override val isLoading: Boolean = false,
//  val createdBookOrder: CreateBookOrderMutation.CreateBookOrder?,
//  override val error: String? = null
// ) : BookOrderUiState(isLoading, error)
//

data class BookOrder(
  public val id: Any,
  public val fullname: String,
  public val address: String?,
  public val city: String?,
  public val district: String?,
  public val state: String?,
  public val pincode: String?,
  public val country: String?,
  public val mobile: String?,
  public val district_officer_name: String?,
  public val district_officer_number: String?,
  public val payment_receipt_url: String?,
  public val created_at: Any?,
  public val is_fulfilled: Boolean?
)

data class BookOrdersUiState(
  override val isLoading: Boolean = false,
  val bookOrders: List<BookOrder>,
  override val error: String? = null
) : BookOrderUiState(isLoading, error)
//
// data class BookOrderDetailsUiState(
//  override val isLoading: Boolean = false,
//  val bookOrder: BookOrderQuery.BookOrder?,
//  override val error: String? = null
// ) : BookOrderUiState(isLoading, error)

class BookOrderViewModel(private val bookOrderRepository: BookOrderRepository) :
  BaseViewModel<DefaultBookOrderUiState>(DefaultBookOrderUiState()) {
//  private val _createBookOrderState = MutableStateFlow<CreateBookOrderUiState>(CreateBookOrderUiState(
//    isLoading = false,
//    createdBookOrder = null,
//    error = null
//  ))
//  val createBookOrderState: StateFlow<CreateBookOrderUiState?> = _createBookOrderState.asStateFlow()
//
//  private val _bookOrdersState = MutableStateFlow<BookOrdersUiState>(BookOrdersUiState(
//    bookOrders = emptyList(),
//    isLoading = false,
//    error = null
//  ))
//  val bookOrdersState: StateFlow<BookOrdersUiState> = _bookOrdersState.asStateFlow()
//
//  private val _bookOrderDetailsState = MutableStateFlow<BookOrderDetailsUiState>(BookOrderDetailsUiState(
//    isLoading = false,
//    bookOrder = null,
//    error = null
//  ))
//  val bookOrderDetailsState: StateFlow<BookOrderDetailsUiState> = _bookOrderDetailsState.asStateFlow()
//
//  init {
//    loadBookOrders()
//  }
//
//  fun updateBookOrdersState(result: Result<List<BookOrdersQuery.BookOrder>>) {
//    when (result) {
//      is Result.Error -> _bookOrdersState.update {
//        it.copy(
//          error = result.message,
//          isLoading = false
//        )
//      }
//
//      is Result.Loading -> _bookOrdersState.update {
//        it.copy(
//          isLoading = true,
//          error = null
//        )
//      }
//
//      is Result.Success -> _bookOrdersState.update {
//        it.copy(
//          bookOrders = result.data,
//          isLoading = false,
//          error = null
//        )
//      }
//    }
//  }
//
//  fun loadBookOrders() {
//    println("BookOrderViewModel: Loading book orders")
//    launch {
//      bookOrderRepository.getBookOrders().collect { result ->
//        println("BookOrderViewModel: Received result: $result")
//        updateBookOrdersState(result)
//        println("BookOrderViewModel: Updated state: ${_bookOrdersState.value}")
//      }
//    }
//  }
//
//  fun createBookOrder(input: BookOrderInput) {
//    launch {
//
//      bookOrderRepository.addBookOrder(input).collect { result ->
//        when (result) {
//          is Result.Success -> _createBookOrderState.value =
//            _createBookOrderState.value.copy(
//              createdBookOrder = result.data,
//              isLoading = false,
//              error = null
//            )
//
//          is Result.Error -> _createBookOrderState.value = _createBookOrderState.value.copy(
//            error = result.message,
//            createdBookOrder = null,
//            isLoading = false,
//          )
//
//          is Result.Loading -> _createBookOrderState.value = _createBookOrderState.value.copy(
//            isLoading = true,
//            createdBookOrder = null,
//            error = null
//          )
//        }
//      }
//    }
//  }
//
//  fun getBookOrderById(id: String) {
//    launch {
//      bookOrderRepository.getBookOrderById(id).collect { result ->
//        when (result) {
//          is Result.Success -> _bookOrderDetailsState.value =
//            _bookOrderDetailsState.value.copy(
//              bookOrder = result.data,
//              error = null,
//              isLoading = false
//            )
//          is Result.Error -> _bookOrderDetailsState.value = _bookOrderDetailsState.value.copy(
//            error = result.message,
//            isLoading = false,
//            bookOrder = null
//          )
//
//          is Result.Loading -> _bookOrderDetailsState.value = _bookOrderDetailsState.value.copy(
//            isLoading = true,
//            bookOrder = null,
//            error = null
//          )
//        }
//      }
//    }
//  }
}
