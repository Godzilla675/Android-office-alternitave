package com.officesuite.app.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the Result utility class.
 */
class ResultTest {

    @Test
    fun `Success result returns correct data`() {
        val result = Result.Success("test data")
        
        assertEquals("test data", result.data)
        assertTrue(result.isSuccess)
        assertFalse(result.isError)
        assertFalse(result.isLoading)
    }

    @Test
    fun `Error result contains exception`() {
        val exception = RuntimeException("test error")
        val result = Result.Error(exception)
        
        assertEquals(exception, result.exception)
        assertTrue(result.isError)
        assertFalse(result.isSuccess)
        assertFalse(result.isLoading)
    }

    @Test
    fun `Loading result is identified correctly`() {
        val result = Result.Loading
        
        assertTrue(result.isLoading)
        assertFalse(result.isSuccess)
        assertFalse(result.isError)
    }

    @Test
    fun `getOrNull returns data for Success`() {
        val result: Result<String> = Result.Success("test")
        
        assertEquals("test", result.getOrNull())
    }

    @Test
    fun `getOrNull returns null for Error`() {
        val result: Result<String> = Result.Error(RuntimeException())
        
        assertNull(result.getOrNull())
    }

    @Test
    fun `getOrNull returns null for Loading`() {
        val result: Result<String> = Result.Loading
        
        assertNull(result.getOrNull())
    }

    @Test
    fun `getOrDefault returns data for Success`() {
        val result: Result<String> = Result.Success("test")
        
        assertEquals("test", result.getOrDefault("default"))
    }

    @Test
    fun `getOrDefault returns default for Error`() {
        val result: Result<String> = Result.Error(RuntimeException())
        
        assertEquals("default", result.getOrDefault("default"))
    }

    @Test
    fun `getOrDefault returns default for Loading`() {
        val result: Result<String> = Result.Loading
        
        assertEquals("default", result.getOrDefault("default"))
    }

    @Test
    fun `getOrThrow returns data for Success`() {
        val result: Result<String> = Result.Success("test")
        
        assertEquals("test", result.getOrThrow())
    }

    @Test(expected = RuntimeException::class)
    fun `getOrThrow throws for Error`() {
        val result: Result<String> = Result.Error(RuntimeException("test error"))
        
        result.getOrThrow()
    }

    @Test(expected = IllegalStateException::class)
    fun `getOrThrow throws for Loading`() {
        val result: Result<String> = Result.Loading
        
        result.getOrThrow()
    }

    @Test
    fun `map transforms Success data`() {
        val result: Result<Int> = Result.Success(5)
        val mapped: Result<String> = result.map { it.toString() }
        
        assertTrue(mapped.isSuccess)
        assertEquals("5", (mapped as Result.Success).data)
    }

    @Test
    fun `map preserves Error`() {
        val exception = RuntimeException("test")
        val result: Result<Int> = Result.Error(exception)
        val mapped: Result<String> = result.map { it.toString() }
        
        assertTrue(mapped.isError)
        assertEquals(exception, (mapped as Result.Error).exception)
    }

    @Test
    fun `map preserves Loading`() {
        val result: Result<Int> = Result.Loading
        val mapped: Result<String> = result.map { it.toString() }
        
        assertTrue(mapped.isLoading)
    }

    @Test
    fun `flatMap transforms Success to new Result`() {
        val result: Result<Int> = Result.Success(5)
        val flatMapped: Result<String> = result.flatMap { Result.Success(it.toString()) }
        
        assertTrue(flatMapped.isSuccess)
        assertEquals("5", (flatMapped as Result.Success).data)
    }

    @Test
    fun `flatMap preserves Error`() {
        val exception = RuntimeException("test")
        val result: Result<Int> = Result.Error(exception)
        val flatMapped: Result<String> = result.flatMap { Result.Success(it.toString()) }
        
        assertTrue(flatMapped.isError)
    }

    @Test
    fun `onSuccess executes action for Success`() {
        var wasExecuted = false
        val result: Result<String> = Result.Success("test")
        
        result.onSuccess { wasExecuted = true }
        
        assertTrue(wasExecuted)
    }

    @Test
    fun `onSuccess does not execute action for Error`() {
        var wasExecuted = false
        val result: Result<String> = Result.Error(RuntimeException())
        
        result.onSuccess { wasExecuted = true }
        
        assertFalse(wasExecuted)
    }

    @Test
    fun `onError executes action for Error`() {
        var wasExecuted = false
        val result: Result<String> = Result.Error(RuntimeException())
        
        result.onError { wasExecuted = true }
        
        assertTrue(wasExecuted)
    }

    @Test
    fun `onError does not execute action for Success`() {
        var wasExecuted = false
        val result: Result<String> = Result.Success("test")
        
        result.onError { wasExecuted = true }
        
        assertFalse(wasExecuted)
    }

    @Test
    fun `runCatching returns Success for successful block`() {
        val result = Result.runCatching { "test" }
        
        assertTrue(result.isSuccess)
        assertEquals("test", (result as Result.Success).data)
    }

    @Test
    fun `runCatching returns Error for throwing block`() {
        val result = Result.runCatching { throw RuntimeException("test error") }
        
        assertTrue(result.isError)
    }

    @Test
    fun `Error userMessage defaults to exception message`() {
        val exception = RuntimeException("custom message")
        val error = Result.Error(exception)
        
        assertEquals("custom message", error.userMessage)
    }

    @Test
    fun `Error userMessage can be customized`() {
        val exception = RuntimeException("technical message")
        val error = Result.Error(exception, "User-friendly message")
        
        assertEquals("User-friendly message", error.userMessage)
    }
}
