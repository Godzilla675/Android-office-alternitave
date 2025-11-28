package com.officesuite.app.productivity

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Scan Presets Manager for saving and loading scanning configurations.
 * Implements Nice-to-Have Feature: Scan Presets from Section 15.
 * 
 * Allows users to:
 * - Save common scanning configurations as presets
 * - Quickly apply presets for different scanning scenarios
 * - Create custom presets for specific use cases
 */
object ScanPresetsManager {
    
    private const val PREFS_NAME = "scan_presets_prefs"
    private const val KEY_PRESETS = "scan_presets"
    private const val KEY_DEFAULT_PRESET = "default_preset"
    private const val KEY_LAST_USED_PRESET = "last_used_preset"
    
    private var prefs: SharedPreferences? = null
    private val gson = Gson()
    
    /**
     * Image filter types available for scanning.
     */
    enum class ImageFilter(val displayName: String) {
        NONE("Original"),
        AUTO("Auto Enhance"),
        GRAYSCALE("Grayscale"),
        BLACK_WHITE("Black & White"),
        HIGH_CONTRAST("High Contrast"),
        MAGIC_COLOR("Magic Color"),
        DOCUMENT("Document Mode")
    }
    
    /**
     * Page size options.
     */
    enum class PageSize(val displayName: String, val widthMm: Float, val heightMm: Float) {
        A4("A4", 210f, 297f),
        LETTER("Letter", 216f, 279f),
        LEGAL("Legal", 216f, 356f),
        A5("A5", 148f, 210f),
        CUSTOM("Custom", 0f, 0f)
    }
    
    /**
     * Scan quality options.
     */
    enum class ScanQuality(val displayName: String, val dpi: Int) {
        LOW("Low (72 DPI)", 72),
        MEDIUM("Medium (150 DPI)", 150),
        HIGH("High (300 DPI)", 300),
        MAXIMUM("Maximum (600 DPI)", 600)
    }
    
    /**
     * Output format options.
     */
    enum class OutputFormat(val displayName: String, val extension: String) {
        PDF("PDF Document", "pdf"),
        JPEG("JPEG Image", "jpg"),
        PNG("PNG Image", "png")
    }
    
    /**
     * Scan preset data class.
     */
    data class ScanPreset(
        val id: String,
        val name: String,
        val description: String,
        val filter: String = ImageFilter.AUTO.name,
        val quality: String = ScanQuality.HIGH.name,
        val pageSize: String = PageSize.A4.name,
        val outputFormat: String = OutputFormat.PDF.name,
        val autoCrop: Boolean = true,
        val autoPerspectiveCorrection: Boolean = true,
        val enhanceColors: Boolean = false,
        val removeShadows: Boolean = true,
        val multiPageMode: Boolean = false,
        val autoSaveEnabled: Boolean = false,
        val autoSavePath: String? = null,
        val runOcrAfterScan: Boolean = false,
        val ocrLanguage: String = "en",
        val isBuiltIn: Boolean = false,
        val createdAt: Long = System.currentTimeMillis(),
        val lastUsedAt: Long = 0
    )
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        ensureBuiltInPresets()
    }
    
    private fun getPrefs(): SharedPreferences {
        return prefs ?: throw IllegalStateException("ScanPresetsManager not initialized. Call init() first.")
    }
    
    /**
     * Ensures built-in presets exist.
     */
    private fun ensureBuiltInPresets() {
        val existingPresets = getAllPresets()
        if (existingPresets.isEmpty()) {
            // Add built-in presets
            val builtInPresets = createBuiltInPresets()
            builtInPresets.forEach { savePreset(it) }
            setDefaultPreset(builtInPresets.first().id)
        }
    }
    
    /**
     * Creates built-in presets for common scanning scenarios.
     */
    private fun createBuiltInPresets(): List<ScanPreset> {
        return listOf(
            ScanPreset(
                id = "document_standard",
                name = "Document (Standard)",
                description = "Standard document scanning with auto-enhancement",
                filter = ImageFilter.DOCUMENT.name,
                quality = ScanQuality.HIGH.name,
                pageSize = PageSize.A4.name,
                outputFormat = OutputFormat.PDF.name,
                autoCrop = true,
                autoPerspectiveCorrection = true,
                removeShadows = true,
                isBuiltIn = true
            ),
            ScanPreset(
                id = "document_ocr",
                name = "Document with OCR",
                description = "Document scanning with text recognition",
                filter = ImageFilter.DOCUMENT.name,
                quality = ScanQuality.HIGH.name,
                pageSize = PageSize.A4.name,
                outputFormat = OutputFormat.PDF.name,
                autoCrop = true,
                autoPerspectiveCorrection = true,
                removeShadows = true,
                runOcrAfterScan = true,
                isBuiltIn = true
            ),
            ScanPreset(
                id = "photo_color",
                name = "Photo (Color)",
                description = "High-quality color photo scanning",
                filter = ImageFilter.AUTO.name,
                quality = ScanQuality.MAXIMUM.name,
                outputFormat = OutputFormat.JPEG.name,
                autoCrop = true,
                autoPerspectiveCorrection = false,
                enhanceColors = true,
                removeShadows = false,
                isBuiltIn = true
            ),
            ScanPreset(
                id = "receipt",
                name = "Receipt",
                description = "Optimized for receipts and small documents",
                filter = ImageFilter.HIGH_CONTRAST.name,
                quality = ScanQuality.MEDIUM.name,
                outputFormat = OutputFormat.PDF.name,
                autoCrop = true,
                autoPerspectiveCorrection = true,
                removeShadows = true,
                runOcrAfterScan = true,
                isBuiltIn = true
            ),
            ScanPreset(
                id = "whiteboard",
                name = "Whiteboard",
                description = "Optimized for whiteboards and presentations",
                filter = ImageFilter.MAGIC_COLOR.name,
                quality = ScanQuality.HIGH.name,
                outputFormat = OutputFormat.PNG.name,
                autoCrop = true,
                autoPerspectiveCorrection = true,
                enhanceColors = true,
                removeShadows = true,
                isBuiltIn = true
            ),
            ScanPreset(
                id = "batch_scan",
                name = "Batch Scan",
                description = "Multi-page document scanning",
                filter = ImageFilter.DOCUMENT.name,
                quality = ScanQuality.HIGH.name,
                pageSize = PageSize.A4.name,
                outputFormat = OutputFormat.PDF.name,
                autoCrop = true,
                autoPerspectiveCorrection = true,
                removeShadows = true,
                multiPageMode = true,
                isBuiltIn = true
            ),
            ScanPreset(
                id = "black_white",
                name = "Black & White",
                description = "Classic black and white scanning",
                filter = ImageFilter.BLACK_WHITE.name,
                quality = ScanQuality.MEDIUM.name,
                outputFormat = OutputFormat.PDF.name,
                autoCrop = true,
                autoPerspectiveCorrection = true,
                isBuiltIn = true
            ),
            ScanPreset(
                id = "id_card",
                name = "ID Card",
                description = "Optimized for ID cards and small documents",
                filter = ImageFilter.AUTO.name,
                quality = ScanQuality.MAXIMUM.name,
                outputFormat = OutputFormat.PNG.name,
                autoCrop = true,
                autoPerspectiveCorrection = true,
                isBuiltIn = true
            )
        )
    }
    
    // ================== Preset Management ==================
    
    /**
     * Saves a preset.
     */
    fun savePreset(preset: ScanPreset) {
        val presets = getAllPresetsMap().toMutableMap()
        presets[preset.id] = preset
        getPrefs().edit().putString(KEY_PRESETS, gson.toJson(presets)).apply()
    }
    
    /**
     * Gets a preset by ID.
     */
    fun getPreset(id: String): ScanPreset? {
        return getAllPresetsMap()[id]
    }
    
    /**
     * Gets all presets as a list, sorted by name.
     */
    fun getAllPresets(): List<ScanPreset> {
        return getAllPresetsMap().values.sortedBy { it.name }
    }
    
    /**
     * Gets all presets as a map.
     */
    private fun getAllPresetsMap(): Map<String, ScanPreset> {
        val json = getPrefs().getString(KEY_PRESETS, null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, ScanPreset>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    /**
     * Deletes a preset.
     * Built-in presets cannot be deleted.
     */
    fun deletePreset(id: String): Boolean {
        val preset = getPreset(id) ?: return false
        if (preset.isBuiltIn) return false
        
        val presets = getAllPresetsMap().toMutableMap()
        presets.remove(id)
        getPrefs().edit().putString(KEY_PRESETS, gson.toJson(presets)).apply()
        return true
    }
    
    /**
     * Creates a custom preset.
     */
    fun createCustomPreset(
        name: String,
        description: String,
        filter: ImageFilter = ImageFilter.AUTO,
        quality: ScanQuality = ScanQuality.HIGH,
        pageSize: PageSize = PageSize.A4,
        outputFormat: OutputFormat = OutputFormat.PDF,
        autoCrop: Boolean = true,
        autoPerspectiveCorrection: Boolean = true,
        enhanceColors: Boolean = false,
        removeShadows: Boolean = true,
        multiPageMode: Boolean = false,
        autoSaveEnabled: Boolean = false,
        autoSavePath: String? = null,
        runOcrAfterScan: Boolean = false,
        ocrLanguage: String = "en"
    ): ScanPreset {
        val id = "custom_${System.currentTimeMillis()}"
        val preset = ScanPreset(
            id = id,
            name = name,
            description = description,
            filter = filter.name,
            quality = quality.name,
            pageSize = pageSize.name,
            outputFormat = outputFormat.name,
            autoCrop = autoCrop,
            autoPerspectiveCorrection = autoPerspectiveCorrection,
            enhanceColors = enhanceColors,
            removeShadows = removeShadows,
            multiPageMode = multiPageMode,
            autoSaveEnabled = autoSaveEnabled,
            autoSavePath = autoSavePath,
            runOcrAfterScan = runOcrAfterScan,
            ocrLanguage = ocrLanguage,
            isBuiltIn = false
        )
        savePreset(preset)
        return preset
    }
    
    /**
     * Duplicates a preset with a new name.
     */
    fun duplicatePreset(presetId: String, newName: String): ScanPreset? {
        val original = getPreset(presetId) ?: return null
        return createCustomPreset(
            name = newName,
            description = original.description,
            filter = ImageFilter.valueOf(original.filter),
            quality = ScanQuality.valueOf(original.quality),
            pageSize = PageSize.valueOf(original.pageSize),
            outputFormat = OutputFormat.valueOf(original.outputFormat),
            autoCrop = original.autoCrop,
            autoPerspectiveCorrection = original.autoPerspectiveCorrection,
            enhanceColors = original.enhanceColors,
            removeShadows = original.removeShadows,
            multiPageMode = original.multiPageMode,
            autoSaveEnabled = original.autoSaveEnabled,
            autoSavePath = original.autoSavePath,
            runOcrAfterScan = original.runOcrAfterScan,
            ocrLanguage = original.ocrLanguage
        )
    }
    
    // ================== Default & Last Used ==================
    
    /**
     * Sets the default preset.
     */
    fun setDefaultPreset(id: String) {
        getPrefs().edit().putString(KEY_DEFAULT_PRESET, id).apply()
    }
    
    /**
     * Gets the default preset.
     */
    fun getDefaultPreset(): ScanPreset? {
        val id = getPrefs().getString(KEY_DEFAULT_PRESET, "document_standard") ?: return null
        return getPreset(id)
    }
    
    /**
     * Records that a preset was used.
     */
    fun recordPresetUsed(id: String) {
        val preset = getPreset(id) ?: return
        val updatedPreset = preset.copy(lastUsedAt = System.currentTimeMillis())
        savePreset(updatedPreset)
        getPrefs().edit().putString(KEY_LAST_USED_PRESET, id).apply()
    }
    
    /**
     * Gets the last used preset.
     */
    fun getLastUsedPreset(): ScanPreset? {
        val id = getPrefs().getString(KEY_LAST_USED_PRESET, null) ?: return null
        return getPreset(id)
    }
    
    /**
     * Gets recently used presets.
     */
    fun getRecentPresets(limit: Int = 5): List<ScanPreset> {
        return getAllPresets()
            .filter { it.lastUsedAt > 0 }
            .sortedByDescending { it.lastUsedAt }
            .take(limit)
    }
    
    /**
     * Gets built-in presets only.
     */
    fun getBuiltInPresets(): List<ScanPreset> {
        return getAllPresets().filter { it.isBuiltIn }
    }
    
    /**
     * Gets custom presets only.
     */
    fun getCustomPresets(): List<ScanPreset> {
        return getAllPresets().filter { !it.isBuiltIn }
    }
    
    /**
     * Resets all presets to built-in defaults.
     */
    fun resetToDefaults() {
        getPrefs().edit().clear().apply()
        ensureBuiltInPresets()
    }
    
    // ================== Utility Methods ==================
    
    /**
     * Gets the ImageFilter enum from a preset.
     */
    fun getImageFilter(preset: ScanPreset): ImageFilter {
        return try {
            ImageFilter.valueOf(preset.filter)
        } catch (e: Exception) {
            ImageFilter.AUTO
        }
    }
    
    /**
     * Gets the ScanQuality enum from a preset.
     */
    fun getScanQuality(preset: ScanPreset): ScanQuality {
        return try {
            ScanQuality.valueOf(preset.quality)
        } catch (e: Exception) {
            ScanQuality.HIGH
        }
    }
    
    /**
     * Gets the PageSize enum from a preset.
     */
    fun getPageSize(preset: ScanPreset): PageSize {
        return try {
            PageSize.valueOf(preset.pageSize)
        } catch (e: Exception) {
            PageSize.A4
        }
    }
    
    /**
     * Gets the OutputFormat enum from a preset.
     */
    fun getOutputFormat(preset: ScanPreset): OutputFormat {
        return try {
            OutputFormat.valueOf(preset.outputFormat)
        } catch (e: Exception) {
            OutputFormat.PDF
        }
    }
}
