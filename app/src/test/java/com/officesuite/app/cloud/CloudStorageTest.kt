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

    @Test
    fun `CloudFile with optional webViewLink`() {
        val file = CloudFile(
            id = "file1",
            name = "test.pdf",
            mimeType = "application/pdf",
            size = 5000L,
            modifiedTime = System.currentTimeMillis(),
            isFolder = false,
            parentId = null,
            webViewLink = "https://example.com/view/file1"
        )
        
        assertEquals("https://example.com/view/file1", file.webViewLink)
        assertNull(file.downloadUrl)
    }

    @Test
    fun `CloudFile with optional downloadUrl`() {
        val file = CloudFile(
            id = "file2",
            name = "document.docx",
            mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            size = 10000L,
            modifiedTime = System.currentTimeMillis(),
            isFolder = false,
            downloadUrl = "https://example.com/download/file2"
        )
        
        assertEquals("https://example.com/download/file2", file.downloadUrl)
    }

    @Test
    fun `CloudFile equality`() {
        val file1 = CloudFile(
            id = "same-id",
            name = "test.pdf",
            mimeType = "application/pdf",
            size = 1024L,
            modifiedTime = 1000L,
            isFolder = false
        )
        val file2 = CloudFile(
            id = "same-id",
            name = "test.pdf",
            mimeType = "application/pdf",
            size = 1024L,
            modifiedTime = 1000L,
            isFolder = false
        )
        
        assertEquals(file1, file2)
    }

    @Test
    fun `CloudResult Success getOrNull returns data`() {
        val data = "test data"
        val result: CloudResult<String> = CloudResult.Success(data)
        
        assertEquals("test data", result.getOrNull())
    }

    @Test
    fun `CloudResult Error getOrNull returns null`() {
        val result: CloudResult<String> = CloudResult.Error("Error occurred")
        
        assertNull(result.getOrNull())
    }

    @Test
    fun `CloudResult Loading getOrNull returns null`() {
        val result: CloudResult<String> = CloudResult.Loading
        
        assertNull(result.getOrNull())
    }

    @Test
    fun `CloudStorageManager SyncResult success`() {
        val result = CloudStorageManager.SyncResult(
            success = true,
            syncedCount = 5,
            errorMessage = null
        )
        
        assertTrue(result.success)
        assertEquals(5, result.syncedCount)
        assertNull(result.errorMessage)
    }

    @Test
    fun `CloudStorageManager SyncResult failure`() {
        val result = CloudStorageManager.SyncResult(
            success = false,
            syncedCount = 0,
            errorMessage = "WiFi required for sync"
        )
        
        assertFalse(result.success)
        assertEquals(0, result.syncedCount)
        assertEquals("WiFi required for sync", result.errorMessage)
    }

    @Test
    fun `CloudStorageManager SyncResult partial success`() {
        val result = CloudStorageManager.SyncResult(
            success = true,
            syncedCount = 3,
            errorMessage = null
        )
        
        assertTrue(result.success)
        assertEquals(3, result.syncedCount)
    }

    @Test
    fun `CloudFile zero size for empty file`() {
        val file = CloudFile(
            id = "empty",
            name = "empty.txt",
            mimeType = "text/plain",
            size = 0L,
            modifiedTime = System.currentTimeMillis(),
            isFolder = false
        )
        
        assertEquals(0L, file.size)
    }

    @Test
    fun `CloudFile large size handling`() {
        val file = CloudFile(
            id = "large",
            name = "large.zip",
            mimeType = "application/zip",
            size = 10L * 1024 * 1024 * 1024, // 10 GB
            modifiedTime = System.currentTimeMillis(),
            isFolder = false
        )
        
        assertEquals(10L * 1024 * 1024 * 1024, file.size)
    }

    @Test
    fun `CloudResult maps correctly`() {
        val success: CloudResult<String> = CloudResult.Success("data")
        val error: CloudResult<String> = CloudResult.Error("error")
        val loading: CloudResult<String> = CloudResult.Loading
        
        assertTrue(success is CloudResult.Success)
        assertTrue(error is CloudResult.Error)
        assertTrue(loading is CloudResult.Loading)
    }

    @Test
    fun `CloudFile copy with modified values`() {
        val original = CloudFile(
            id = "original",
            name = "original.pdf",
            mimeType = "application/pdf",
            size = 1000L,
            modifiedTime = 1000L,
            isFolder = false
        )
        
        val modified = original.copy(name = "modified.pdf", size = 2000L)
        
        assertEquals("original", modified.id)
        assertEquals("modified.pdf", modified.name)
        assertEquals(2000L, modified.size)
    }
}
