package com.aryamahasangh.repository

interface BookOrderRepository {
//  fun getBookOrders(): Flow<Result<List<BookOrdersQuery.BookOrder>>>
//  fun getBookOrderById(id: String): Flow<Result<BookOrderQuery.BookOrder>>
//  fun addBookOrder(bookOrderDetails: BookOrderInput): Flow<Result<CreateBookOrderMutation.CreateBookOrder>>
}

class BookOrderRepositoryImpl : BookOrderRepository {
//  override fun getBookOrders(): Flow<Result<List<BookOrdersQuery.BookOrder>>>  = flow {
//    emit(Result.Loading)
//
//    val result = safeCall {
//      val response = apolloClient.query(BookOrdersQuery()).execute()
//      if (response.hasErrors()) {
//        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
//      }
//      response.data?.bookOrders ?: emptyList()
//    }
//    println("repo $result")
//    emit(result)
//  }
//
//  override fun getBookOrderById(id: String): Flow<Result<BookOrderQuery.BookOrder>> = flow {
//    emit(Result.Loading)
//
//    val result = safeCall {
//      val response = apolloClient.query(BookOrderQuery(id)).execute()
//      if (response.hasErrors()) {
//        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
//      }
//      response.data?.bookOrder ?: throw Exception("Book order not found")
//    }
//
//    emit(result)
//  }
//
//  override fun addBookOrder(bookOrderDetails: BookOrderInput): Flow<Result<CreateBookOrderMutation.CreateBookOrder>> = flow {
//    emit(Result.Loading)
//
//    val result = safeCall {
//      val response = apolloClient.mutation(CreateBookOrderMutation(bookOrderDetails)).execute()
//      if (response.hasErrors()) {
//        throw Exception(response.errors?.firstOrNull()?.message ?: "Unknown error occurred")
//      }
//      response.data?.createBookOrder ?: throw Exception("Book order couldn't be created")
//    }
//
//    emit(result)
//  }
}
