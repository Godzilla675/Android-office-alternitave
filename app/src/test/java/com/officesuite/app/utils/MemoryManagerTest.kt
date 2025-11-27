package com.officesuite.app.utils

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the MemoryManager utility class.
 */
class MemoryManagerTest {

    @Before
    fun setUp() {
        // Clear cache before each test
        MemoryManager.clearCache()
        MemoryManager.resetStats()
    }

    @Test
    fun `createPageKey generates correct format`() {
        val key = MemoryManager.createPageKey("/path/to/document.pdf", 5)
        
        assertEquals("/path/to/document.pdf:page:5", key)
    }

    @Test
    fun `createSlideKey generates correct format`() {
        val key = MemoryManager.createSlideKey("/path/to/presentation.pptx", 3)
        
        assertEquals("/path/to/presentation.pptx:slide:3", key)
    }

    @Test
    fun `getStats returns valid statistics`() {
        val stats = MemoryManager.getStats()
        
        assertNotNull(stats)
        // In unit tests, maxSize might be 0 due to mocked Runtime
        assertTrue(stats.maxSize >= 0)
        assertEquals(0, stats.currentSize)
        assertEquals(0, stats.hitCount)
        assertEquals(0, stats.missCount)
    }

    @Test
    fun `getBitmap returns null for non-existent key`() {
        val bitmap = MemoryManager.getBitmap("non-existent-key")
        
        assertNull(bitmap)
    }

    @Test
    fun `getHitRate returns zero when no operations`() {
        val hitRate = MemoryManager.getHitRate()
        
        assertEquals(0f, hitRate, 0.001f)
    }

    @Test
    fun `clearCache resets cache size to zero`() {
        MemoryManager.clearCache()
        
        assertEquals(0, MemoryManager.getCacheSize())
    }

    @Test
    fun `getMaxCacheSize returns non-negative value`() {
        val maxSize = MemoryManager.getMaxCacheSize()
        
        // In unit tests, maxSize might be 0 due to mocked Runtime
        assertTrue(maxSize >= 0)
    }

    @Test
    fun `resetStats clears hit and miss counts`() {
        MemoryManager.resetStats()
        val stats = MemoryManager.getStats()
        
        assertEquals(0, stats.hitCount)
        assertEquals(0, stats.missCount)
        assertEquals(0f, stats.hitRate, 0.001f)
    }

    @Test
    fun `recycleBitmap handles null bitmap safely`() {
        // Should not throw
        MemoryManager.recycleBitmap(null)
    }

    @Test
    fun `recycleBitmaps handles null list safely`() {
        // Should not throw
        MemoryManager.recycleBitmaps(null)
    }

    @Test
    fun `recycleBitmaps handles empty list safely`() {
        // Should not throw
        MemoryManager.recycleBitmaps(emptyList())
    }

    @Test
    fun `CacheStats data class holds values correctly`() {
        val stats = MemoryManager.CacheStats(
            currentSize = 100,
            maxSize = 1000,
            hitCount = 50,
            missCount = 10,
            hitRate = 83.33f
        )
        
        assertEquals(100, stats.currentSize)
        assertEquals(1000, stats.maxSize)
        assertEquals(50, stats.hitCount)
        assertEquals(10, stats.missCount)
        assertEquals(83.33f, stats.hitRate, 0.01f)
    }
}
