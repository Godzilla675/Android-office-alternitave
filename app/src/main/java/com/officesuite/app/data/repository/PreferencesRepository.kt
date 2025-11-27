package com.officesuite.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.officesuite.app.data.model.DocumentFile
import com.officesuite.app.data.model.DocumentType

/**
 * Repository for managing favorites, recent files, and user preferences.
 * Implements Nice-to-Have Features #11 (File Management Enhancements) and #14 (Customization & Themes)
 */
class PreferencesRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    // ================== Favorites ==================
    
    fun addFavorite(uri: String, name: String, type: DocumentType) {
        val favorites = getFavorites().toMutableList()
        if (favorites.none { it.uri.toString() == uri }) {
            favorites.add(FavoriteItem(uri, name, type.name, System.currentTimeMillis()))
            saveFavorites(favorites)
        }
    }

    fun removeFavorite(uri: String) {
        val favorites = getFavorites().filter { it.uri != uri }
        saveFavorites(favorites)
    }

    fun isFavorite(uri: String): Boolean {
        return getFavorites().any { it.uri == uri }
    }

    fun getFavorites(): List<FavoriteItem> {
        val json = prefs.getString(KEY_FAVORITES, null) ?: return emptyList()
        val type = object : TypeToken<List<FavoriteItem>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveFavorites(favorites: List<FavoriteItem>) {
        prefs.edit().putString(KEY_FAVORITES, gson.toJson(favorites)).apply()
    }

    // ================== Recent Files ==================
    
    fun addRecentFile(uri: String, name: String, type: DocumentType, size: Long) {
        val recentFiles = getRecentFiles().toMutableList()
        // Remove if already exists (to move to top)
        recentFiles.removeAll { it.uri == uri }
        // Add at the beginning
        recentFiles.add(0, RecentFileItem(uri, name, type.name, size, System.currentTimeMillis()))
        // Keep only last 20 files
        val trimmed = recentFiles.take(20)
        saveRecentFiles(trimmed)
    }

    fun getRecentFiles(): List<RecentFileItem> {
        val json = prefs.getString(KEY_RECENT_FILES, null) ?: return emptyList()
        val type = object : TypeToken<List<RecentFileItem>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearRecentFiles() {
        prefs.edit().remove(KEY_RECENT_FILES).apply()
    }

    private fun saveRecentFiles(files: List<RecentFileItem>) {
        prefs.edit().putString(KEY_RECENT_FILES, gson.toJson(files)).apply()
    }

    // ================== Theme Preferences ==================
    
    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    var isReadingMode: Boolean
        get() = prefs.getBoolean(KEY_READING_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_READING_MODE, value).apply()

    var fontSize: FontSize
        get() {
            val ordinal = prefs.getInt(KEY_FONT_SIZE, FontSize.MEDIUM.ordinal)
            return FontSize.values().getOrElse(ordinal) { FontSize.MEDIUM }
        }
        set(value) = prefs.edit().putInt(KEY_FONT_SIZE, value.ordinal).apply()

    // ================== Auto-save Preferences ==================
    
    var isAutoSaveEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SAVE, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SAVE, value).apply()

    var autoSaveIntervalSeconds: Int
        get() = prefs.getInt(KEY_AUTO_SAVE_INTERVAL, 30)
        set(value) = prefs.edit().putInt(KEY_AUTO_SAVE_INTERVAL, value).apply()

    // ================== Text-to-Speech Preferences ==================
    
    var ttsSpeed: Float
        get() = prefs.getFloat(KEY_TTS_SPEED, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_TTS_SPEED, value).apply()

    var ttsPitch: Float
        get() = prefs.getFloat(KEY_TTS_PITCH, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_TTS_PITCH, value).apply()

    companion object {
        private const val PREFS_NAME = "office_suite_prefs"
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_RECENT_FILES = "recent_files"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_READING_MODE = "reading_mode"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_AUTO_SAVE = "auto_save"
        private const val KEY_AUTO_SAVE_INTERVAL = "auto_save_interval"
        private const val KEY_TTS_SPEED = "tts_speed"
        private const val KEY_TTS_PITCH = "tts_pitch"
    }
}

data class FavoriteItem(
    val uri: String,
    val name: String,
    val type: String,
    val addedAt: Long
)

data class RecentFileItem(
    val uri: String,
    val name: String,
    val type: String,
    val size: Long,
    val accessedAt: Long
)

enum class FontSize(val scaleFactor: Float) {
    SMALL(0.85f),
    MEDIUM(1.0f),
    LARGE(1.15f),
    EXTRA_LARGE(1.3f)
}
