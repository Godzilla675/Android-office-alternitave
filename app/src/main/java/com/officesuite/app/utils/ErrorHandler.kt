package com.officesuite.app.utils

import android.content.Context
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import android.view.View
import java.io.FileNotFoundException
import java.io.IOException
import java.net.UnknownHostException
import java.security.GeneralSecurityException

/**
 * Centralized error handler that provides user-friendly error messages.
 * Handles common error types and provides consistent error display.
 */
object ErrorHandler {
    
    /**
     * Error types that the app can encounter.
     */
    enum class ErrorType {
        FILE_NOT_FOUND,
        FILE_READ_ERROR,
        FILE_WRITE_ERROR,
        UNSUPPORTED_FORMAT,
        MEMORY_ERROR,
        NETWORK_ERROR,
        PERMISSION_DENIED,
        CONVERSION_ERROR,
        OCR_ERROR,
        CAMERA_ERROR,
        SECURITY_ERROR,
        UNKNOWN
    }
    
    /**
     * Converts an exception to a user-friendly error message.
     */
    fun getErrorMessage(throwable: Throwable): String {
        return when (throwable) {
            is FileNotFoundException -> "File not found. It may have been moved or deleted."
            is UnknownHostException -> "No internet connection. Please check your network."
            is IOException -> "Unable to read or write the file. Please check file permissions."
            is OutOfMemoryError -> "Not enough memory to process this file. Try closing other apps."
            is SecurityException -> "Permission denied. Please grant the required permissions."
            is GeneralSecurityException -> "Security error. The file may be encrypted or corrupted."
            is UnsupportedOperationException -> "This operation is not supported for this file type."
            is IllegalArgumentException -> "Invalid file or input provided."
            is IllegalStateException -> "The app encountered an unexpected state. Please try again."
            else -> throwable.localizedMessage ?: "An unexpected error occurred."
        }
    }
    
    /**
     * Converts an exception to an ErrorType.
     */
    fun getErrorType(throwable: Throwable): ErrorType {
        return when (throwable) {
            is FileNotFoundException -> ErrorType.FILE_NOT_FOUND
            is UnknownHostException -> ErrorType.NETWORK_ERROR
            is IOException -> ErrorType.FILE_READ_ERROR
            is OutOfMemoryError -> ErrorType.MEMORY_ERROR
            is SecurityException -> ErrorType.PERMISSION_DENIED
            is GeneralSecurityException -> ErrorType.SECURITY_ERROR
            is UnsupportedOperationException -> ErrorType.UNSUPPORTED_FORMAT
            else -> ErrorType.UNKNOWN
        }
    }
    
    /**
     * Gets a user-friendly message for an error type.
     */
    fun getMessageForType(type: ErrorType): String {
        return when (type) {
            ErrorType.FILE_NOT_FOUND -> "File not found. It may have been moved or deleted."
            ErrorType.FILE_READ_ERROR -> "Unable to read the file. Please try again."
            ErrorType.FILE_WRITE_ERROR -> "Unable to save the file. Check available storage."
            ErrorType.UNSUPPORTED_FORMAT -> "This file format is not supported."
            ErrorType.MEMORY_ERROR -> "Not enough memory. Try closing other apps."
            ErrorType.NETWORK_ERROR -> "Network error. Check your internet connection."
            ErrorType.PERMISSION_DENIED -> "Permission required. Please grant access in settings."
            ErrorType.CONVERSION_ERROR -> "Unable to convert the file. Please try a different format."
            ErrorType.OCR_ERROR -> "Text recognition failed. Try with a clearer image."
            ErrorType.CAMERA_ERROR -> "Camera error. Please check camera permissions."
            ErrorType.SECURITY_ERROR -> "Security error. The file may be protected."
            ErrorType.UNKNOWN -> "An unexpected error occurred. Please try again."
        }
    }
    
    /**
     * Shows an error toast with a user-friendly message.
     */
    fun showErrorToast(context: Context, throwable: Throwable, duration: Int = Toast.LENGTH_LONG) {
        val message = getErrorMessage(throwable)
        Toast.makeText(context, message, duration).show()
    }
    
    /**
     * Shows an error toast with a specific error type.
     */
    fun showErrorToast(context: Context, errorType: ErrorType, duration: Int = Toast.LENGTH_LONG) {
        val message = getMessageForType(errorType)
        Toast.makeText(context, message, duration).show()
    }
    
    /**
     * Shows an error snackbar with a user-friendly message.
     */
    fun showErrorSnackbar(
        view: View,
        throwable: Throwable,
        duration: Int = Snackbar.LENGTH_LONG,
        actionText: String? = null,
        action: (() -> Unit)? = null
    ) {
        val message = getErrorMessage(throwable)
        val snackbar = Snackbar.make(view, message, duration)
        
        if (actionText != null && action != null) {
            snackbar.setAction(actionText) { action() }
        }
        
        snackbar.show()
    }
    
    /**
     * Shows an error snackbar with a specific error type.
     */
    fun showErrorSnackbar(
        view: View,
        errorType: ErrorType,
        duration: Int = Snackbar.LENGTH_LONG,
        actionText: String? = null,
        action: (() -> Unit)? = null
    ) {
        val message = getMessageForType(errorType)
        val snackbar = Snackbar.make(view, message, duration)
        
        if (actionText != null && action != null) {
            snackbar.setAction(actionText) { action() }
        }
        
        snackbar.show()
    }
    
    /**
     * Logs an error with appropriate context.
     */
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        val errorType = throwable?.let { getErrorType(it) } ?: ErrorType.UNKNOWN
        android.util.Log.e(tag, "[$errorType] $message", throwable)
    }
    
    /**
     * Creates a Result.Error with a user-friendly message.
     */
    fun <T> createError(throwable: Throwable): Result<T> {
        return Result.Error(throwable, getErrorMessage(throwable))
    }
}
