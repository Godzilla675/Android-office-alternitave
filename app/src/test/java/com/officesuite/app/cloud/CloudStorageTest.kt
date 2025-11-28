package com.officesuite.app.cloud

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for CloudStorageProvider and CloudResult classes.
 */
class CloudStorageTest {

    @Test
    fun `CloudFile stores properties correctly`() {
        val file = CloudFile(
            id = "file123",
            name = "document.pdf",
            mimeType = "application/pdf",
            size = 1024L,
            modifiedTime = System.currentTimeMillis(),
            isFolder = false,
            parentId = "folder456"
        )
        
        assertEquals("file123", file.id)
        assertEquals("document.pdf", file.name)
        assertEquals("application/pdf", file.mimeType)
        assertEquals(1024L, file.size)
        assertFalse(file.isFolder)
        assertEquals("folder456", file.parentId)
    }

    @Test
    fun `CloudFile folder representation`() {
        val folder = CloudFile(
            id = "folder123",
            name = "Documents",
            mimeType = "application/directory",
            size = 0L,
            modifiedTime = System.currentTimeMillis(),
            isFolder = true
        )
        
        assertTrue(folder.isFolder)
        assertNull(folder.parentId)
    }

    @Test
    fun `CloudResult Success state`() {
        val data = listOf(
            CloudFile(
                id = "1",
                name = "file1.pdf",
                mimeType = "application/pdf",
                size = 100L,
                modifiedTime = 0L,
                isFolder = false
            )
        )
        val result: CloudResult<List<CloudFile>> = CloudResult.Success(data)
        
        assertTrue(result.isSuccess)
        assertFalse(result.isError)
        assertEquals(data, result.getOrNull())
        assertNull(result.errorMessageOrNull())
    }

    @Test
    fun `CloudResult Error state`() {
        val result: CloudResult<List<CloudFile>> = CloudResult.Error("Network error")
        
        assertFalse(result.isSuccess)
        assertTrue(result.isError)
        assertNull(result.getOrNull())
        assertEquals("Network error", result.errorMessageOrNull())
    }

    @Test
    fun `CloudResult Error with exception`() {
        val exception = Exception("Connection failed")
        val result: CloudResult<Unit> = CloudResult.Error("Connection failed", exception)
        
        assertTrue(result.isError)
        assertEquals("Connection failed", result.errorMessageOrNull())
    }

    @Test
    fun `CloudResult Loading state`() {
        val result: CloudResult<String> = CloudResult.Loading
        
        assertFalse(result.isSuccess)
        assertFalse(result.isError)
        assertNull(result.getOrNull())
        assertNull(result.errorMessageOrNull())
    }

    @Test
    fun `CloudStorageManager ProviderType enum values`() {
        val types = CloudStorageManager.ProviderType.values()
        
        assertEquals(4, types.size)
        assertTrue(types.contains(CloudStorageManager.ProviderType.LOCAL))
        assertTrue(types.contains(CloudStorageManager.ProviderType.GOOGLE_DRIVE))
        assertTrue(types.contains(CloudStorageManager.ProviderType.DROPBOX))
        assertTrue(types.contains(CloudStorageManager.ProviderType.ONEDRIVE))
    }
}
