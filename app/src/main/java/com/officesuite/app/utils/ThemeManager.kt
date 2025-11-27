package com.officesuite.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate

/**
 * Theme manager for handling app themes including Material You dynamic colors support.
 * Provides centralized theme management with persistent preferences.
 */
object ThemeManager {
    
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_DYNAMIC_COLORS = "dynamic_colors"
    
    /**
     * Available theme modes.
     */
    enum class ThemeMode(val value: Int) {
        LIGHT(AppCompatDelegate.MODE_NIGHT_NO),
        DARK(AppCompatDelegate.MODE_NIGHT_YES),
        SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        
        companion object {
            fun fromValue(value: Int): ThemeMode {
                return entries.find { it.value == value } ?: SYSTEM
            }
        }
    }
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Gets the current theme mode.
     */
    fun getThemeMode(context: Context): ThemeMode {
        val value = getPrefs(context).getInt(KEY_THEME_MODE, ThemeMode.SYSTEM.value)
        return ThemeMode.fromValue(value)
    }
    
    /**
     * Sets the theme mode.
     */
    fun setThemeMode(context: Context, mode: ThemeMode) {
        getPrefs(context).edit().putInt(KEY_THEME_MODE, mode.value).apply()
        AppCompatDelegate.setDefaultNightMode(mode.value)
    }
    
    /**
     * Checks if dynamic colors are enabled.
     */
    fun isDynamicColorsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DYNAMIC_COLORS, true) && supportsDynamicColors()
    }
    
    /**
     * Sets whether dynamic colors are enabled.
     */
    fun setDynamicColorsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DYNAMIC_COLORS, enabled).apply()
    }
    
    /**
     * Checks if the device supports Material You dynamic colors.
     */
    fun supportsDynamicColors(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }
    
    /**
     * Applies the saved theme settings.
     * Should be called early in the app lifecycle (e.g., Application.onCreate).
     */
    fun applyTheme(context: Context) {
        val mode = getThemeMode(context)
        AppCompatDelegate.setDefaultNightMode(mode.value)
    }
    
    /**
     * Toggles between light and dark mode.
     */
    fun toggleDarkMode(context: Context) {
        val currentMode = getThemeMode(context)
        val newMode = when (currentMode) {
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.SYSTEM -> {
                // Check current system state and toggle to the opposite
                val nightModeFlags = context.resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK
                if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                    ThemeMode.LIGHT
                } else {
                    ThemeMode.DARK
                }
            }
        }
        setThemeMode(context, newMode)
    }
    
    /**
     * Checks if the app is currently in dark mode.
     */
    fun isDarkMode(context: Context): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }
}
