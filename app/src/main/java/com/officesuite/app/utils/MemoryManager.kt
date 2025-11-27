package com.officesuite.app.utils

import android.graphics.Bitmap
import android.util.LruCache

/**
 * Memory manager for efficient caching and management of bitmaps and other large objects.
 * Uses an LRU (Least Recently Used) cache strategy to maintain optimal memory usage.
 * 
 * Features:
 * - Automatic memory management with LRU eviction
 * - Size-based caching limits
 * - Safe bitmap recycling
 * - Statistics tracking
 */
object MemoryManager {
    
    // Use 1/8th of the available memory for caching
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8
    
    private val bitmapCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            // Return the size of the bitmap in kilobytes
            return bitmap.byteCount / 1024
        }
        
        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: Bitmap,
            newValue: Bitmap?
        ) {
            // Recycle the old bitmap when it's evicted from cache
            // Only recycle if it's being evicted (not replaced) and not recycled already
            if (evicted && !oldValue.isRecycled) {
                oldValue.recycle()
            }
        }
    }
    
    private var hitCount = 0
    private var missCount = 0
    
    /**
     * Gets a bitmap from the cache.
     * @param key The unique identifier for the bitmap
     * @return The cached bitmap, or null if not found or recycled
     */
    fun getBitmap(key: String): Bitmap? {
        val bitmap = bitmapCache.get(key)
        return if (bitmap != null && !bitmap.isRecycled) {
            hitCount++
            bitmap
        } else {
            missCount++
            // Remove recycled bitmap from cache
            if (bitmap != null) {
                bitmapCache.remove(key)
            }
            null
        }
    }
    
    /**
     * Puts a bitmap into the cache.
     * @param key The unique identifier for the bitmap
     * @param bitmap The bitmap to cache
     */
    fun putBitmap(key: String, bitmap: Bitmap) {
        if (!bitmap.isRecycled) {
            bitmapCache.put(key, bitmap)
        }
    }
    
    /**
     * Removes a specific bitmap from the cache.
     * @param key The unique identifier for the bitmap
     */
    fun removeBitmap(key: String) {
        bitmapCache.remove(key)
    }
    
    /**
     * Clears all cached bitmaps.
     */
    fun clearCache() {
        bitmapCache.evictAll()
    }
    
    /**
     * Gets the current cache size in KB.
     */
    fun getCacheSize(): Int = bitmapCache.size()
    
    /**
     * Gets the maximum cache size in KB.
     */
    fun getMaxCacheSize(): Int = bitmapCache.maxSize()
    
    /**
     * Gets cache hit rate as a percentage.
     */
    fun getHitRate(): Float {
        val total = hitCount + missCount
        return if (total > 0) {
            (hitCount.toFloat() / total) * 100
        } else {
            0f
        }
    }
    
    /**
     * Gets cache statistics.
     */
    fun getStats(): CacheStats {
        return CacheStats(
            currentSize = bitmapCache.size(),
            maxSize = bitmapCache.maxSize(),
            hitCount = hitCount,
            missCount = missCount,
            hitRate = getHitRate()
        )
    }
    
    /**
     * Resets cache statistics.
     */
    fun resetStats() {
        hitCount = 0
        missCount = 0
    }
    
    data class CacheStats(
        val currentSize: Int,
        val maxSize: Int,
        val hitCount: Int,
        val missCount: Int,
        val hitRate: Float
    )
    
    /**
     * Safely recycles a bitmap if it's not null and not already recycled.
     */
    fun recycleBitmap(bitmap: Bitmap?) {
        if (bitmap != null && !bitmap.isRecycled) {
            bitmap.recycle()
        }
    }
    
    /**
     * Safely recycles a list of bitmaps.
     */
    fun recycleBitmaps(bitmaps: List<Bitmap>?) {
        bitmaps?.forEach { recycleBitmap(it) }
    }
    
    /**
     * Creates a bitmap key for a document page.
     */
    fun createPageKey(documentPath: String, pageIndex: Int): String {
        return "$documentPath:page:$pageIndex"
    }
    
    /**
     * Creates a bitmap key for a slide.
     */
    fun createSlideKey(documentPath: String, slideIndex: Int): String {
        return "$documentPath:slide:$slideIndex"
    }
    
    /**
     * Checks if the app is running low on memory.
     */
    fun isLowMemory(): Boolean {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024
        val maxMemory = runtime.maxMemory() / 1024
        // Consider low memory if we're using more than 80% of max heap
        return usedMemory > maxMemory * 0.8
    }
    
    /**
     * Trims cache if memory is running low.
     */
    fun trimCacheIfNeeded() {
        if (isLowMemory()) {
            // Trim cache to half its size
            bitmapCache.trimToSize(bitmapCache.maxSize() / 2)
        }
    }
}
