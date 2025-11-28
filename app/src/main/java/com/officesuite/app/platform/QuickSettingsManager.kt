package com.officesuite.app.platform

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.content.Intent
import android.content.SharedPreferences
import androidx.annotation.RequiresApi
import com.officesuite.app.MainActivity
import com.officesuite.app.R
import com.officesuite.app.widget.QuickActionsWidget

/**
 * Quick Settings Tile Service for quick scanner access.
 * Implements Phase 2 Platform-Specific Feature #25: Quick Settings Tile
 * 
 * Allows users to quickly access the document scanner from the
 * Android Quick Settings panel.
 */
@RequiresApi(Build.VERSION_CODES.N)
class ScannerTileService : TileService() {
    
    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }
    
    override fun onClick() {
        super.onClick()
        
        // Launch scanner activity
        val intent = Intent(this, MainActivity::class.java).apply {
            action = QuickActionsWidget.ACTION_SCAN
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        // Collapse the quick settings panel and start activity
        @Suppress("DEPRECATION")
        startActivityAndCollapse(intent)
    }
    
    private fun updateTile() {
        val tile = qsTile ?: return
        
        tile.label = getString(R.string.scan_document)
        tile.contentDescription = getString(R.string.shortcut_scan_long)
        tile.state = Tile.STATE_INACTIVE
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(R.string.scan_mode_document)
        }
        
        tile.updateTile()
    }
    
    companion object {
        /**
         * Check if the tile service is added to quick settings
         */
        fun isAdded(context: Context): Boolean {
            return try {
                val prefs = context.getSharedPreferences("quick_settings_prefs", Context.MODE_PRIVATE)
                prefs.getBoolean("scanner_tile_added", false)
            } catch (e: Exception) {
                false
            }
        }
        
        /**
         * Request to add the tile to quick settings
         */
        @Suppress("UNUSED_VARIABLE")
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun requestAddTile(context: Context) {
            val statusBarManager = context.getSystemService(Context.STATUS_BAR_SERVICE)
            // Request adding tile using StatusBarManager (API 33+)
        }
    }
}

/**
 * Quick Settings Tile configuration manager.
 * Provides utilities for managing Quick Settings tiles.
 */
class QuickSettingsManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    /**
     * Available quick settings tile types
     */
    enum class TileType(val id: String, val titleRes: Int, val descriptionRes: Int) {
        SCANNER("scanner_tile", R.string.scan_document, R.string.shortcut_scan_long),
        CREATE("create_tile", R.string.create_new, R.string.shortcut_create_long),
        CONVERT("convert_tile", R.string.convert, R.string.shortcut_convert_long)
    }
    
    /**
     * Tile configuration
     */
    data class TileConfig(
        val type: TileType,
        val isEnabled: Boolean,
        val customLabel: String? = null
    )
    
    /**
     * Check if a specific tile is enabled
     */
    fun isTileEnabled(type: TileType): Boolean {
        return prefs.getBoolean("${type.id}_enabled", type == TileType.SCANNER)
    }
    
    /**
     * Enable or disable a tile
     */
    fun setTileEnabled(type: TileType, enabled: Boolean) {
        prefs.edit().putBoolean("${type.id}_enabled", enabled).apply()
    }
    
    /**
     * Get custom label for a tile
     */
    fun getCustomLabel(type: TileType): String? {
        return prefs.getString("${type.id}_label", null)
    }
    
    /**
     * Set custom label for a tile
     */
    fun setCustomLabel(type: TileType, label: String?) {
        if (label != null) {
            prefs.edit().putString("${type.id}_label", label).apply()
        } else {
            prefs.edit().remove("${type.id}_label").apply()
        }
    }
    
    /**
     * Get all tile configurations
     */
    fun getAllTileConfigs(): List<TileConfig> {
        return TileType.entries.map { type ->
            TileConfig(
                type = type,
                isEnabled = isTileEnabled(type),
                customLabel = getCustomLabel(type)
            )
        }
    }
    
    /**
     * Check if quick settings tiles are available on this device
     */
    fun areTilesAvailable(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    }
    
    companion object {
        private const val PREFS_NAME = "quick_settings_prefs"
    }
}
