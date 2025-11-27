package com.officesuite.app.security

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.ReaderProperties
import com.itextpdf.kernel.pdf.WriterProperties
import com.itextpdf.kernel.pdf.EncryptionConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Manager for PDF security features including password protection and encryption.
 * Uses iText library for PDF encryption operations.
 */
class PdfSecurityManager {

    /**
     * Result class for security operations
     */
    data class SecurityResult(
        val success: Boolean,
        val outputPath: String? = null,
        val errorMessage: String? = null
    )

    /**
     * Password protection options
     */
    data class PasswordOptions(
        val userPassword: String? = null,
        val ownerPassword: String,
        val allowPrinting: Boolean = true,
        val allowCopy: Boolean = false,
        val allowModification: Boolean = false,
        val allowAnnotation: Boolean = true,
        val encryptionStrength: EncryptionStrength = EncryptionStrength.AES_256
    )

    /**
     * Encryption strength options
     */
    enum class EncryptionStrength(val value: Int) {
        AES_128(EncryptionConstants.ENCRYPTION_AES_128),
        AES_256(EncryptionConstants.ENCRYPTION_AES_256)
    }

    /**
     * Encrypt a PDF file with password protection.
     * 
     * @param inputFile The source PDF file
     * @param outputFile The output encrypted PDF file
     * @param options Password and encryption options
     * @return SecurityResult indicating success or failure
     */
    suspend fun encryptPdf(
        inputFile: File,
        outputFile: File,
        options: PasswordOptions
    ): SecurityResult = withContext(Dispatchers.IO) {
        try {
            val reader = PdfReader(FileInputStream(inputFile))
            
            val writerProps = WriterProperties()
            
            // Build permissions
            var permissions = 0
            if (options.allowPrinting) {
                permissions = permissions or EncryptionConstants.ALLOW_PRINTING
            }
            if (options.allowCopy) {
                permissions = permissions or EncryptionConstants.ALLOW_COPY
            }
            if (options.allowModification) {
                permissions = permissions or EncryptionConstants.ALLOW_MODIFY_CONTENTS
            }
            if (options.allowAnnotation) {
                permissions = permissions or EncryptionConstants.ALLOW_MODIFY_ANNOTATIONS
            }
            
            writerProps.setStandardEncryption(
                options.userPassword?.toByteArray(),
                options.ownerPassword.toByteArray(),
                permissions,
                options.encryptionStrength.value
            )
            
            val writer = PdfWriter(FileOutputStream(outputFile), writerProps)
            val pdfDocument = PdfDocument(reader, writer)
            
            pdfDocument.close()
            
            SecurityResult(
                success = true,
                outputPath = outputFile.absolutePath
            )
        } catch (e: Exception) {
            SecurityResult(
                success = false,
                errorMessage = e.message ?: "Failed to encrypt PDF"
            )
        }
    }

    /**
     * Remove password protection from a PDF file.
     * 
     * @param inputFile The encrypted PDF file
     * @param outputFile The output decrypted PDF file
     * @param password The password to unlock the PDF
     * @return SecurityResult indicating success or failure
     */
    suspend fun decryptPdf(
        inputFile: File,
        outputFile: File,
        password: String
    ): SecurityResult = withContext(Dispatchers.IO) {
        try {
            val readerProps = ReaderProperties().setPassword(password.toByteArray())
            val reader = PdfReader(FileInputStream(inputFile), readerProps)
            val writer = PdfWriter(FileOutputStream(outputFile))
            
            val pdfDocument = PdfDocument(reader, writer)
            pdfDocument.close()
            
            SecurityResult(
                success = true,
                outputPath = outputFile.absolutePath
            )
        } catch (e: Exception) {
            SecurityResult(
                success = false,
                errorMessage = e.message ?: "Failed to decrypt PDF"
            )
        }
    }

    /**
     * Check if a PDF file is encrypted.
     * 
     * @param file The PDF file to check
     * @return true if the file is encrypted, false otherwise
     */
    suspend fun isEncrypted(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val reader = PdfReader(FileInputStream(file))
            val encrypted = reader.isEncrypted
            reader.close()
            encrypted
        } catch (e: Exception) {
            // If we get a BadPasswordException, the file is encrypted
            e.message?.contains("password", ignoreCase = true) == true
        }
    }

    /**
     * Verify if a password is correct for an encrypted PDF.
     * 
     * @param file The encrypted PDF file
     * @param password The password to verify
     * @return true if the password is correct, false otherwise
     */
    suspend fun verifyPassword(file: File, password: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val readerProps = ReaderProperties().setPassword(password.toByteArray())
            val reader = PdfReader(FileInputStream(file), readerProps)
            reader.close()
            true
        } catch (e: Exception) {
            false
        }
    }
}
