package com.officesuite.app.performance

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the PerformanceMonitor utility class.
 */
class PerformanceMonitorTest {

    @Test
    fun `getCurrentFps returns non-negative value`() {
        val fps = PerformanceMonitor.getCurrentFps()
        assertTrue("FPS should be non-negative", fps >= 0f)
    }

    @Test
    fun `recordFrameTime tracks frame times`() {
        // Record multiple frames
        PerformanceMonitor.recordFrameTime()
        Thread.sleep(16) // Simulate 60 FPS frame time
        PerformanceMonitor.recordFrameTime()
        Thread.sleep(16)
        PerformanceMonitor.recordFrameTime()
        
        // FPS should be around 60 (allowing for test timing variance)
        val fps = PerformanceMonitor.getCurrentFps()
        assertTrue("FPS should be positive after recording frames", fps > 0)
    }

    @Test
    fun `getHeapMemoryUsage returns valid values`() {
        val heapInfo = PerformanceMonitor.getHeapMemoryUsage()
        
        assertTrue("Used bytes should be positive", heapInfo.usedBytes > 0)
        assertTrue("Max bytes should be positive", heapInfo.maxBytes > 0)
        assertTrue("Used bytes should be less than max", heapInfo.usedBytes <= heapInfo.maxBytes)
        assertTrue("Used percent should be between 0 and 1", heapInfo.usedPercent in 0f..1f)
        assertTrue("Available bytes should be non-negative", heapInfo.availableBytes >= 0)
    }

    @Test
    fun `isRenderingSmooth returns false with no frames`() {
        val smooth = PerformanceMonitor.isRenderingSmooth()
        assertFalse(smooth)
    }

    @Test
    fun `HeapMemoryInfo data class holds values correctly`() {
        val info = HeapMemoryInfo(
            usedBytes = 100_000_000,
            maxBytes = 500_000_000,
            usedPercent = 0.2f,
            availableBytes = 400_000_000
        )
        
        assertEquals(100_000_000, info.usedBytes)
        assertEquals(500_000_000, info.maxBytes)
        assertEquals(0.2f, info.usedPercent, 0.001f)
        assertEquals(400_000_000, info.availableBytes)
    }

    @Test
    fun `PerformanceMetrics data class holds values correctly`() {
        val metrics = PerformanceMetrics(
            heapUsedMb = 100,
            heapMaxMb = 500,
            heapUsedPercent = 0.2f,
            systemAvailableMb = 1000,
            systemTotalMb = 2000,
            fps = 60f,
            isHardwareAccelerated = true,
            timestamp = System.currentTimeMillis()
        )
        
        assertEquals(100L, metrics.heapUsedMb)
        assertEquals(500L, metrics.heapMaxMb)
        assertEquals(0.2f, metrics.heapUsedPercent, 0.001f)
        assertEquals(60f, metrics.fps, 0.001f)
        assertTrue(metrics.isHardwareAccelerated)
    }

    @Test
    fun `PerformanceRecommendation data class holds values correctly`() {
        val recommendation = PerformanceRecommendation(
            type = RecommendationType.MEMORY,
            priority = Priority.HIGH,
            message = "Test message",
            action = "Test action"
        )
        
        assertEquals(RecommendationType.MEMORY, recommendation.type)
        assertEquals(Priority.HIGH, recommendation.priority)
        assertEquals("Test message", recommendation.message)
        assertEquals("Test action", recommendation.action)
    }

    @Test
    fun `RecommendationType enum has expected values`() {
        val types = RecommendationType.values()
        
        assertTrue(types.contains(RecommendationType.MEMORY))
        assertTrue(types.contains(RecommendationType.RENDERING))
        assertTrue(types.contains(RecommendationType.GPU))
        assertTrue(types.contains(RecommendationType.STORAGE))
        assertTrue(types.contains(RecommendationType.NETWORK))
    }

    @Test
    fun `Priority enum has expected values`() {
        val priorities = Priority.values()
        
        assertEquals(3, priorities.size)
        assertTrue(priorities.contains(Priority.LOW))
        assertTrue(priorities.contains(Priority.MEDIUM))
        assertTrue(priorities.contains(Priority.HIGH))
    }
}
