package com.officesuite.app.performance

import android.content.Context
import android.graphics.Typeface
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Lazy Font Loading Manager.
 * Implements Technical Improvements Phase 2 - Section 21: Lazy Font Loading
 * 
 * Features:
 * - On-demand font loading to reduce app size
 * - Font caching for performance
 * - Async font loading with callbacks
 * - Font fallback handling
 */
object LazyFontManager {
    
    private const val TAG = "LazyFontManager"
    
    // Cache for loaded fonts
    private val fontCache = ConcurrentHashMap<String, Typeface>()
    
    // Loading state for fonts
    private val loadingFonts = ConcurrentHashMap<String, MutableStateFlow<FontLoadState>>()
    
    // Scope for async operations
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Available fonts (can be extended for downloadable fonts)
    private val availableFonts = mapOf(
        // System fonts
        "default" to FontInfo("default", "Default", null, isSystem = true),
        "sans-serif" to FontInfo("sans-serif", "Sans Serif", null, isSystem = true),
        "serif" to FontInfo("serif", "Serif", null, isSystem = true),
        "monospace" to FontInfo("monospace", "Monospace", null, isSystem = true),
        
        // Custom fonts (paths relative to assets/fonts)
        "roboto-regular" to FontInfo("roboto-regular", "Roboto", "fonts/roboto_regular.ttf"),
        "roboto-bold" to FontInfo("roboto-bold", "Roboto Bold", "fonts/roboto_bold.ttf"),
        "roboto-italic" to FontInfo("roboto-italic", "Roboto Italic", "fonts/roboto_italic.ttf"),
        "roboto-mono" to FontInfo("roboto-mono", "Roboto Mono", "fonts/roboto_mono.ttf"),
        "open-sans" to FontInfo("open-sans", "Open Sans", "fonts/open_sans.ttf"),
        "lato" to FontInfo("lato", "Lato", "fonts/lato.ttf"),
        "opendyslexic" to FontInfo("opendyslexic", "OpenDyslexic", "fonts/opendyslexic.ttf"),
        "source-code-pro" to FontInfo("source-code-pro", "Source Code Pro", "fonts/source_code_pro.ttf")
    )
    
    // Fallback chain
    private val fallbackChain = listOf("default", "sans-serif", "serif")
    
    /**
     * Get a font, loading it lazily if necessary.
     * Returns immediately with cached font or default, then loads async.
     */
    fun getFont(
        context: Context,
        fontId: String,
        onLoaded: ((Typeface) -> Unit)? = null
    ): Typeface {
        // Return from cache if available
        fontCache[fontId]?.let { cached ->
            onLoaded?.invoke(cached)
            return cached
        }
        
        // Get font info
        val fontInfo = availableFonts[fontId]
        
        // If system font, load immediately
        if (fontInfo?.isSystem == true) {
            val typeface = loadSystemFont(fontId)
            fontCache[fontId] = typeface
            onLoaded?.invoke(typeface)
            return typeface
        }
        
        // Start async loading if not already loading
        if (!loadingFonts.containsKey(fontId)) {
            loadFontAsync(context, fontId, fontInfo)
        }
        
        // Subscribe to loading state
        if (onLoaded != null) {
            scope.launch {
                loadingFonts[fontId]?.collect { state ->
                    if (state is FontLoadState.Loaded) {
                        withContext(Dispatchers.Main) {
                            onLoaded(state.typeface)
                        }
                    }
                }
            }
        }
        
        // Return default font immediately
        return getDefaultFont()
    }
    
    /**
     * Get a font synchronously, blocking until loaded.
     */
    suspend fun getFontSync(context: Context, fontId: String): Typeface {
        // Return from cache if available
        fontCache[fontId]?.let { return it }
        
        // Load font
        return loadFont(context, fontId)
    }
    
    /**
     * Preload fonts for faster access later.
     */
    fun preloadFonts(context: Context, fontIds: List<String>) {
        scope.launch {
            fontIds.forEach { fontId ->
                loadFont(context, fontId)
            }
        }
    }
    
    /**
     * Get loading state for a font.
     */
    fun getFontLoadState(fontId: String): StateFlow<FontLoadState>? {
        return loadingFonts[fontId]
    }
    
    /**
     * Check if a font is loaded.
     */
    fun isFontLoaded(fontId: String): Boolean {
        return fontCache.containsKey(fontId)
    }
    
    /**
     * Get all available fonts.
     */
    fun getAvailableFonts(): List<FontInfo> {
        return availableFonts.values.toList()
    }
    
    /**
     * Get font info by ID.
     */
    fun getFontInfo(fontId: String): FontInfo? {
        return availableFonts[fontId]
    }
    
    /**
     * Clear font cache to free memory.
     */
    fun clearCache() {
        fontCache.clear()
        loadingFonts.clear()
    }
    
    /**
     * Get the default system font.
     */
    fun getDefaultFont(): Typeface {
        return fontCache["default"] ?: Typeface.DEFAULT
    }
    
    /**
     * Get cache statistics.
     */
    fun getCacheStats(): FontCacheStats {
        return FontCacheStats(
            loadedFonts = fontCache.size,
            availableFonts = availableFonts.size,
            loadingFonts = loadingFonts.count { 
                it.value.value is FontLoadState.Loading 
            }
        )
    }
    
    private fun loadFontAsync(context: Context, fontId: String, fontInfo: FontInfo?) {
        val stateFlow = MutableStateFlow<FontLoadState>(FontLoadState.Loading)
        loadingFonts[fontId] = stateFlow
        
        scope.launch {
            try {
                val typeface = loadFont(context, fontId)
                stateFlow.value = FontLoadState.Loaded(typeface)
            } catch (e: Exception) {
                stateFlow.value = FontLoadState.Error(e.message ?: "Failed to load font")
            }
        }
    }
    
    private suspend fun loadFont(context: Context, fontId: String): Typeface {
        return withContext(Dispatchers.IO) {
            // Check cache again
            fontCache[fontId]?.let { return@withContext it }
            
            val fontInfo = availableFonts[fontId]
            
            val typeface = when {
                fontInfo == null -> getDefaultFont()
                fontInfo.isSystem -> loadSystemFont(fontId)
                fontInfo.assetPath != null -> loadAssetFont(context, fontInfo.assetPath)
                else -> getDefaultFont()
            }
            
            fontCache[fontId] = typeface
            typeface
        }
    }
    
    private fun loadSystemFont(fontId: String): Typeface {
        return when (fontId) {
            "default" -> Typeface.DEFAULT
            "sans-serif" -> Typeface.SANS_SERIF
            "serif" -> Typeface.SERIF
            "monospace" -> Typeface.MONOSPACE
            else -> Typeface.DEFAULT
        }
    }
    
    private fun loadAssetFont(context: Context, assetPath: String): Typeface {
        return try {
            Typeface.createFromAsset(context.assets, assetPath)
        } catch (e: Exception) {
            // Try fallback fonts
            for (fallbackId in fallbackChain) {
                try {
                    return loadSystemFont(fallbackId)
                } catch (e2: Exception) {
                    continue
                }
            }
            Typeface.DEFAULT
        }
    }
    
    fun shutdown() {
        scope.cancel()
    }
}

/**
 * Font information.
 */
data class FontInfo(
    val id: String,
    val displayName: String,
    val assetPath: String?,
    val isSystem: Boolean = false,
    val category: FontCategory = FontCategory.SANS_SERIF,
    val weight: Int = 400,
    val isItalic: Boolean = false
)

/**
 * Font category.
 */
enum class FontCategory {
    SERIF,
    SANS_SERIF,
    MONOSPACE,
    DISPLAY,
    HANDWRITING,
    ACCESSIBILITY
}

/**
 * Font loading state.
 */
sealed class FontLoadState {
    object Loading : FontLoadState()
    data class Loaded(val typeface: Typeface) : FontLoadState()
    data class Error(val message: String) : FontLoadState()
}

/**
 * Font cache statistics.
 */
data class FontCacheStats(
    val loadedFonts: Int,
    val availableFonts: Int,
    val loadingFonts: Int
)
