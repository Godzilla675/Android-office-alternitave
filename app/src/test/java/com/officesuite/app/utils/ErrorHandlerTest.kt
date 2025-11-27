package com.officesuite.app.utils

import org.junit.Assert.*
import org.junit.Test
import java.io.FileNotFoundException
import java.io.IOException
import java.net.UnknownHostException

/**
 * Unit tests for the ErrorHandler utility class.
 */
class ErrorHandlerTest {

    @Test
    fun `getErrorMessage returns user-friendly message for FileNotFoundException`() {
        val exception = FileNotFoundException("test.pdf")
        val message = ErrorHandler.getErrorMessage(exception)
        
        assertTrue(message.contains("File not found"))
    }

    @Test
    fun `getErrorMessage returns user-friendly message for IOException`() {
        val exception = IOException("read error")
        val message = ErrorHandler.getErrorMessage(exception)
        
        assertTrue(message.contains("read") || message.contains("write") || message.contains("file"))
    }

    @Test
    fun `getErrorMessage returns user-friendly message for OutOfMemoryError`() {
        val error = OutOfMemoryError("heap exhausted")
        val message = ErrorHandler.getErrorMessage(error)
        
        assertTrue(message.contains("memory"))
    }

    @Test
    fun `getErrorMessage returns user-friendly message for UnknownHostException`() {
        val exception = UnknownHostException("example.com")
        val message = ErrorHandler.getErrorMessage(exception)
        
        // The message contains "No internet" with capital N
        assertTrue("Expected message about network/internet, got: $message",
            message.lowercase().contains("internet") || message.lowercase().contains("network"))
    }

    @Test
    fun `getErrorMessage returns user-friendly message for SecurityException`() {
        val exception = SecurityException("permission denied")
        val message = ErrorHandler.getErrorMessage(exception)
        
        assertTrue(message.contains("Permission") || message.contains("permission"))
    }

    @Test
    fun `getErrorMessage returns user-friendly message for UnsupportedOperationException`() {
        val exception = UnsupportedOperationException("not supported")
        val message = ErrorHandler.getErrorMessage(exception)
        
        assertTrue(message.contains("not supported"))
    }

    @Test
    fun `getErrorMessage returns localized message for unknown exception`() {
        val exception = RuntimeException("custom error message")
        val message = ErrorHandler.getErrorMessage(exception)
        
        assertEquals("custom error message", message)
    }

    @Test
    fun `getErrorMessage returns default message for exception without message`() {
        val exception = RuntimeException()
        val message = ErrorHandler.getErrorMessage(exception)
        
        assertNotNull(message)
        assertTrue(message.isNotEmpty())
    }

    @Test
    fun `getErrorType returns FILE_NOT_FOUND for FileNotFoundException`() {
        val exception = FileNotFoundException()
        val type = ErrorHandler.getErrorType(exception)
        
        assertEquals(ErrorHandler.ErrorType.FILE_NOT_FOUND, type)
    }

    @Test
    fun `getErrorType returns FILE_READ_ERROR for IOException`() {
        val exception = IOException()
        val type = ErrorHandler.getErrorType(exception)
        
        assertEquals(ErrorHandler.ErrorType.FILE_READ_ERROR, type)
    }

    @Test
    fun `getErrorType returns MEMORY_ERROR for OutOfMemoryError`() {
        val error = OutOfMemoryError()
        val type = ErrorHandler.getErrorType(error)
        
        assertEquals(ErrorHandler.ErrorType.MEMORY_ERROR, type)
    }

    @Test
    fun `getErrorType returns NETWORK_ERROR for UnknownHostException`() {
        val exception = UnknownHostException()
        val type = ErrorHandler.getErrorType(exception)
        
        assertEquals(ErrorHandler.ErrorType.NETWORK_ERROR, type)
    }

    @Test
    fun `getErrorType returns PERMISSION_DENIED for SecurityException`() {
        val exception = SecurityException()
        val type = ErrorHandler.getErrorType(exception)
        
        assertEquals(ErrorHandler.ErrorType.PERMISSION_DENIED, type)
    }

    @Test
    fun `getErrorType returns UNSUPPORTED_FORMAT for UnsupportedOperationException`() {
        val exception = UnsupportedOperationException()
        val type = ErrorHandler.getErrorType(exception)
        
        assertEquals(ErrorHandler.ErrorType.UNSUPPORTED_FORMAT, type)
    }

    @Test
    fun `getErrorType returns UNKNOWN for unrecognized exception`() {
        val exception = RuntimeException()
        val type = ErrorHandler.getErrorType(exception)
        
        assertEquals(ErrorHandler.ErrorType.UNKNOWN, type)
    }

    @Test
    fun `getMessageForType returns non-empty message for all error types`() {
        for (type in ErrorHandler.ErrorType.entries) {
            val message = ErrorHandler.getMessageForType(type)
            
            assertNotNull(message)
            assertTrue("Message for $type should not be empty", message.isNotEmpty())
        }
    }

    @Test
    fun `createError returns Result Error with user-friendly message`() {
        val exception = FileNotFoundException("test.pdf")
        val result: Result<String> = ErrorHandler.createError(exception)
        
        assertTrue(result.isError)
        val error = result as Result.Error
        assertEquals(exception, error.exception)
        assertTrue(error.userMessage.contains("File not found"))
    }

    @Test
    fun `ErrorType enum contains expected values`() {
        val expectedTypes = listOf(
            ErrorHandler.ErrorType.FILE_NOT_FOUND,
            ErrorHandler.ErrorType.FILE_READ_ERROR,
            ErrorHandler.ErrorType.FILE_WRITE_ERROR,
            ErrorHandler.ErrorType.UNSUPPORTED_FORMAT,
            ErrorHandler.ErrorType.MEMORY_ERROR,
            ErrorHandler.ErrorType.NETWORK_ERROR,
            ErrorHandler.ErrorType.PERMISSION_DENIED,
            ErrorHandler.ErrorType.CONVERSION_ERROR,
            ErrorHandler.ErrorType.OCR_ERROR,
            ErrorHandler.ErrorType.CAMERA_ERROR,
            ErrorHandler.ErrorType.SECURITY_ERROR,
            ErrorHandler.ErrorType.UNKNOWN
        )
        
        assertEquals(expectedTypes.size, ErrorHandler.ErrorType.entries.size)
        for (type in expectedTypes) {
            assertTrue(ErrorHandler.ErrorType.entries.contains(type))
        }
    }
}
