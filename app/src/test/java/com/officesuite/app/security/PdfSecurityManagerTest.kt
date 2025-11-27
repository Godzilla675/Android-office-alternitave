package com.officesuite.app.security

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for PdfSecurityManager class.
 */
class PdfSecurityManagerTest {

    @Test
    fun `PasswordOptions default values are correct`() {
        val options = PdfSecurityManager.PasswordOptions(
            ownerPassword = "test123"
        )
        
        assertNull(options.userPassword)
        assertEquals("test123", options.ownerPassword)
        assertTrue(options.allowPrinting)
        assertFalse(options.allowCopy)
        assertFalse(options.allowModification)
        assertTrue(options.allowAnnotation)
        assertEquals(PdfSecurityManager.EncryptionStrength.AES_256, options.encryptionStrength)
    }

    @Test
    fun `PasswordOptions with custom values`() {
        val options = PdfSecurityManager.PasswordOptions(
            userPassword = "user123",
            ownerPassword = "owner456",
            allowPrinting = false,
            allowCopy = true,
            allowModification = true,
            allowAnnotation = false,
            encryptionStrength = PdfSecurityManager.EncryptionStrength.AES_128
        )
        
        assertEquals("user123", options.userPassword)
        assertEquals("owner456", options.ownerPassword)
        assertFalse(options.allowPrinting)
        assertTrue(options.allowCopy)
        assertTrue(options.allowModification)
        assertFalse(options.allowAnnotation)
        assertEquals(PdfSecurityManager.EncryptionStrength.AES_128, options.encryptionStrength)
    }

    @Test
    fun `SecurityResult success state`() {
        val result = PdfSecurityManager.SecurityResult(
            success = true,
            outputPath = "/path/to/output.pdf"
        )
        
        assertTrue(result.success)
        assertEquals("/path/to/output.pdf", result.outputPath)
        assertNull(result.errorMessage)
    }

    @Test
    fun `SecurityResult failure state`() {
        val result = PdfSecurityManager.SecurityResult(
            success = false,
            errorMessage = "Failed to encrypt"
        )
        
        assertFalse(result.success)
        assertNull(result.outputPath)
        assertEquals("Failed to encrypt", result.errorMessage)
    }

    @Test
    fun `EncryptionStrength values are defined`() {
        val aes128 = PdfSecurityManager.EncryptionStrength.AES_128
        val aes256 = PdfSecurityManager.EncryptionStrength.AES_256
        
        assertNotNull(aes128)
        assertNotNull(aes256)
        assertNotEquals(aes128.value, aes256.value)
    }

    @Test
    fun `PdfSecurityManager can be instantiated`() {
        val manager = PdfSecurityManager()
        assertNotNull(manager)
    }
}
