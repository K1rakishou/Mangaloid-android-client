package com.github.mangaloid.client.core.data_structure

sealed class AsyncData<T> {
  class NotInitialized<T> : AsyncData<T>()
  class Loading<T> : AsyncData<T>()
  data class Error<T>(val throwable: Throwable) : AsyncData<T>()
  data class Data<T>(val data: T) : AsyncData<T>()
}