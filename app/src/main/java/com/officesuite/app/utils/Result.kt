package com.officesuite.app.utils

/**
 * A sealed class representing either a successful result or an error.
 * Provides a type-safe way to handle operations that can fail.
 * 
 * @param T The type of the successful result
 */
sealed class Result<out T> {
    /**
     * Represents a successful result containing the value.
     */
    data class Success<out T>(val data: T) : Result<T>()
    
    /**
     * Represents an error with an exception and optional user-friendly message.
     */
    data class Error(
        val exception: Throwable,
        val userMessage: String = exception.localizedMessage ?: "An error occurred"
    ) : Result<Nothing>()
    
    /**
     * Represents a loading state.
     */
    object Loading : Result<Nothing>()
    
    /**
     * Returns true if this is a successful result.
     */
    val isSuccess: Boolean get() = this is Success
    
    /**
     * Returns true if this is an error result.
     */
    val isError: Boolean get() = this is Error
    
    /**
     * Returns true if this is a loading state.
     */
    val isLoading: Boolean get() = this is Loading
    
    /**
     * Returns the data if successful, or null otherwise.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
    
    /**
     * Returns the data if successful, or the default value otherwise.
     */
    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> default
    }
    
    /**
     * Returns the data if successful, or throws the exception if error.
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
        is Loading -> throw IllegalStateException("Result is still loading")
    }
    
    /**
     * Maps the successful result to a new type.
     */
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> Loading
    }
    
    /**
     * Flat maps the successful result to a new Result.
     */
    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(data)
        is Error -> this
        is Loading -> Loading
    }
    
    /**
     * Executes the given action if this is a success.
     */
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }
    
    /**
     * Executes the given action if this is an error.
     */
    inline fun onError(action: (Error) -> Unit): Result<T> {
        if (this is Error) action(this)
        return this
    }
    
    companion object {
        /**
         * Creates a Result from a block that may throw an exception.
         */
        inline fun <T> runCatching(block: () -> T): Result<T> {
            return try {
                Success(block())
            } catch (e: Throwable) {
                Error(e)
            }
        }
        
        /**
         * Creates a Result from a suspend block that may throw an exception.
         */
        suspend inline fun <T> runCatchingSuspend(crossinline block: suspend () -> T): Result<T> {
            return try {
                Success(block())
            } catch (e: Throwable) {
                Error(e)
            }
        }
    }
}
