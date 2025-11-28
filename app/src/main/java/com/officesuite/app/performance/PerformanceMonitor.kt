package com.officesuite.app.performance

import android.app.ActivityManager
import android.content.Context
import android.graphics.HardwareRenderer
import android.os.Build
import android.os.Debug
import android.view.View
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Performance Monitor for GPU Acceleration and Optimization.
 * Implements Technical Improvements Phase 2 - Section 21: GPU Acceleration & Performance
 * 
 * Features:
 * - Hardware acceleration configuration
 * - Memory usage monitoring
 * - Frame rate tracking
 * - Performance metrics collection
 * - Lazy loading support
 */
object PerformanceMonitor {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics
    
    private val frameTimeHistory = ConcurrentLinkedQueue<Long>()
    private var lastFrameTime = 0L
    private var isMonitoring = false
    
    // Performance thresholds
    private const val MAX_FRAME_HISTORY = 120
    private const val TARGET_FRAME_TIME_MS = 16L // 60 FPS
    private const val MEMORY_WARNING_THRESHOLD = 0.8f
    private const val MEMORY_CRITICAL_THRESHOLD = 0.9f
    
    /**
     * Start performance monitoring.
     */
    fun startMonitoring(context: Context) {
        if (isMonitoring) return
        isMonitoring = true
        
        scope.launch {
            while (isMonitoring) {
                collectMetrics(context)
                delay(1000) // Update every second
            }
        }
    }
    
    /**
     * Stop performance monitoring.
     */
    fun stopMonitoring() {
        isMonitoring = false
    }
    
    /**
     * Record a frame time for FPS calculation.
     */
    fun recordFrameTime() {
        val currentTime = System.nanoTime()
        if (lastFrameTime != 0L) {
            val frameTime = (currentTime - lastFrameTime) / 1_000_000 // Convert to ms
            frameTimeHistory.add(frameTime)
            
            // Keep only recent frames
            while (frameTimeHistory.size > MAX_FRAME_HISTORY) {
                frameTimeHistory.poll()
            }
        }
        lastFrameTime = currentTime
    }
    
    /**
     * Enable hardware acceleration for a view.
     */
    fun enableHardwareAcceleration(view: View) {
        view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }
    
    /**
     * Disable hardware acceleration for memory-constrained situations.
     */
    fun disableHardwareAcceleration(view: View) {
        view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }
    
    /**
     * Configure optimal rendering settings based on device capabilities.
     */
    fun configureOptimalRendering(view: View, context: Context) {
        val memInfo = getMemoryInfo(context)
        
        when {
            memInfo.availMem < memInfo.totalMem * 0.2 -> {
                // Low memory - use software rendering
                view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            }
            memInfo.lowMemory -> {
                // Memory warning - use none layer type
                view.setLayerType(View.LAYER_TYPE_NONE, null)
            }
            else -> {
                // Normal - use hardware acceleration
                view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            }
        }
    }
    
    /**
     * Check if GPU acceleration is available and recommended.
     */
    fun isGpuAccelerationRecommended(context: Context): Boolean {
        val memInfo = getMemoryInfo(context)
        return !memInfo.lowMemory && memInfo.availMem > memInfo.totalMem * 0.3
    }
    
    /**
     * Get current memory info.
     */
    fun getMemoryInfo(context: Context): ActivityManager.MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo
    }
    
    /**
     * Get app-specific heap memory usage.
     */
    fun getHeapMemoryUsage(): HeapMemoryInfo {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        
        return HeapMemoryInfo(
            usedBytes = usedMemory,
            maxBytes = maxMemory,
            usedPercent = usedMemory.toFloat() / maxMemory,
            availableBytes = maxMemory - usedMemory
        )
    }
    
    /**
     * Check if memory is critically low.
     */
    fun isMemoryCritical(): Boolean {
        val heapInfo = getHeapMemoryUsage()
        return heapInfo.usedPercent >= MEMORY_CRITICAL_THRESHOLD
    }
    
    /**
     * Check if memory warning should be shown.
     */
    fun isMemoryWarning(): Boolean {
        val heapInfo = getHeapMemoryUsage()
        return heapInfo.usedPercent >= MEMORY_WARNING_THRESHOLD
    }
    
    /**
     * Request garbage collection when needed.
     */
    fun requestGcIfNeeded() {
        if (isMemoryWarning()) {
            System.gc()
        }
    }
    
    /**
     * Calculate current FPS.
     */
    fun getCurrentFps(): Float {
        if (frameTimeHistory.isEmpty()) return 0f
        
        val avgFrameTime = frameTimeHistory.average()
        return if (avgFrameTime > 0) (1000.0 / avgFrameTime).toFloat() else 0f
    }
    
    /**
     * Check if rendering is smooth (above 30 FPS).
     */
    fun isRenderingSmooth(): Boolean {
        return getCurrentFps() >= 30f
    }
    
    /**
     * Get performance recommendations.
     */
    fun getPerformanceRecommendations(context: Context): List<PerformanceRecommendation> {
        val recommendations = mutableListOf<PerformanceRecommendation>()
        
        val heapInfo = getHeapMemoryUsage()
        val fps = getCurrentFps()
        
        if (heapInfo.usedPercent > 0.7f) {
            recommendations.add(
                PerformanceRecommendation(
                    type = RecommendationType.MEMORY,
                    priority = if (heapInfo.usedPercent > 0.9f) Priority.HIGH else Priority.MEDIUM,
                    message = "Memory usage is high (${(heapInfo.usedPercent * 100).toInt()}%). Consider closing unused documents.",
                    action = "Clear cache or close documents"
                )
            )
        }
        
        if (fps in 1f..30f) {
            recommendations.add(
                PerformanceRecommendation(
                    type = RecommendationType.RENDERING,
                    priority = Priority.MEDIUM,
                    message = "Low frame rate detected (${fps.toInt()} FPS). Rendering may be slow.",
                    action = "Reduce document complexity or zoom out"
                )
            )
        }
        
        if (!isGpuAccelerationRecommended(context)) {
            recommendations.add(
                PerformanceRecommendation(
                    type = RecommendationType.GPU,
                    priority = Priority.LOW,
                    message = "GPU acceleration is disabled due to memory constraints.",
                    action = "Free up memory to enable hardware acceleration"
                )
            )
        }
        
        return recommendations
    }
    
    private fun collectMetrics(context: Context) {
        val heapInfo = getHeapMemoryUsage()
        val systemMemInfo = getMemoryInfo(context)
        val fps = getCurrentFps()
        
        _performanceMetrics.value = PerformanceMetrics(
            heapUsedMb = heapInfo.usedBytes / (1024 * 1024),
            heapMaxMb = heapInfo.maxBytes / (1024 * 1024),
            heapUsedPercent = heapInfo.usedPercent,
            systemAvailableMb = systemMemInfo.availMem / (1024 * 1024),
            systemTotalMb = systemMemInfo.totalMem / (1024 * 1024),
            fps = fps,
            isHardwareAccelerated = true, // Assuming hardware acceleration is on
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Log performance snapshot for debugging.
     */
    fun logPerformanceSnapshot() {
        val metrics = _performanceMetrics.value
        val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        
        android.util.Log.d("PerformanceMonitor", """
            |=== Performance Snapshot ===
            |Time: ${dateFormat.format(Date(metrics.timestamp))}
            |Heap: ${metrics.heapUsedMb}MB / ${metrics.heapMaxMb}MB (${(metrics.heapUsedPercent * 100).toInt()}%)
            |System Memory: ${metrics.systemAvailableMb}MB available of ${metrics.systemTotalMb}MB
            |FPS: ${metrics.fps}
            |Hardware Accelerated: ${metrics.isHardwareAccelerated}
            |============================
        """.trimMargin())
    }
    
    fun shutdown() {
        isMonitoring = false
        scope.cancel()
    }
}

/**
 * Performance metrics data class.
 */
data class PerformanceMetrics(
    val heapUsedMb: Long = 0,
    val heapMaxMb: Long = 0,
    val heapUsedPercent: Float = 0f,
    val systemAvailableMb: Long = 0,
    val systemTotalMb: Long = 0,
    val fps: Float = 0f,
    val isHardwareAccelerated: Boolean = false,
    val timestamp: Long = 0
)

/**
 * Heap memory info.
 */
data class HeapMemoryInfo(
    val usedBytes: Long,
    val maxBytes: Long,
    val usedPercent: Float,
    val availableBytes: Long
)

/**
 * Performance recommendation.
 */
data class PerformanceRecommendation(
    val type: RecommendationType,
    val priority: Priority,
    val message: String,
    val action: String
)

enum class RecommendationType {
    MEMORY,
    RENDERING,
    GPU,
    STORAGE,
    NETWORK
}

enum class Priority {
    LOW,
    MEDIUM,
    HIGH
}
