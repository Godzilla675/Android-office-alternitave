package com.officesuite.app.platform

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

/**
 * Manages Nearby Share functionality for quick document sharing.
 * Implements Phase 2 Platform-Specific Feature #25: Nearby Share
 * 
 * Enables users to quickly share documents with nearby devices using
 * Android's Nearby Share feature.
 */
class NearbyShareManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    /**
     * Share target information
     */
    data class ShareTarget(
        val name: String,
        val deviceType: DeviceType,
        val isRecent: Boolean
    )
    
    /**
     * Device types for sharing
     */
    enum class DeviceType {
        PHONE,
        TABLET,
        COMPUTER,
        TV,
        UNKNOWN
    }
    
    /**
     * Share result
     */
    sealed class ShareResult {
        data object Success : ShareResult()
        data class Error(val message: String) : ShareResult()
        data object Cancelled : ShareResult()
        data object NearbyShareUnavailable : ShareResult()
    }
    
    /**
     * Share preferences
     */
    data class SharePreferences(
        val autoAcceptFromContacts: Boolean = false,
        val deviceVisibility: DeviceVisibility = DeviceVisibility.CONTACTS_ONLY,
        val showQuickShareOption: Boolean = true
    )
    
    /**
     * Device visibility options
     */
    enum class DeviceVisibility(val title: String) {
        EVERYONE("Everyone"),
        CONTACTS_ONLY("Contacts only"),
        HIDDEN("Hidden")
    }
    
    /**
     * Recent share info
     */
    data class RecentShare(
        val fileName: String,
        val targetDevice: String,
        val timestamp: Long,
        val wasSuccessful: Boolean
    )
    
    /**
     * Check if Nearby Share is available on this device
     */
    fun isNearbyShareAvailable(): Boolean {
        // Nearby Share is available on Android 6.0+ with Google Play Services
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }
    
    /**
     * Share a file using Nearby Share
     */
    fun shareFile(file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return createShareIntent(uri, getMimeType(file.name))
    }
    
    /**
     * Share a document URI using Nearby Share
     */
    fun shareUri(uri: Uri, mimeType: String = "*/*"): Intent {
        return createShareIntent(uri, mimeType)
    }
    
    /**
     * Share multiple files using Nearby Share
     */
    fun shareMultipleFiles(files: List<File>): Intent {
        val uris = ArrayList<Uri>()
        files.forEach { file ->
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            uris.add(uri)
        }
        
        return Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.google.android.gms") // Target Google Play Services for Nearby Share
        }
    }
    
    /**
     * Create share intent with Nearby Share preference
     */
    private fun createShareIntent(uri: Uri, mimeType: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            
            // Try to use Nearby Share directly if available
            if (isNearbyShareAvailable()) {
                setPackage("com.google.android.gms")
            }
        }
    }
    
    /**
     * Create a quick share chooser intent
     */
    fun createQuickShareChooser(uri: Uri, mimeType: String = "*/*", title: String? = null): Intent {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        return Intent.createChooser(shareIntent, title ?: "Share document")
    }
    
    /**
     * Get share preferences
     */
    fun getSharePreferences(): SharePreferences {
        return SharePreferences(
            autoAcceptFromContacts = prefs.getBoolean(KEY_AUTO_ACCEPT, false),
            deviceVisibility = DeviceVisibility.entries.find {
                it.name == prefs.getString(KEY_VISIBILITY, DeviceVisibility.CONTACTS_ONLY.name)
            } ?: DeviceVisibility.CONTACTS_ONLY,
            showQuickShareOption = prefs.getBoolean(KEY_SHOW_QUICK_SHARE, true)
        )
    }
    
    /**
     * Save share preferences
     */
    fun saveSharePreferences(preferences: SharePreferences) {
        prefs.edit().apply {
            putBoolean(KEY_AUTO_ACCEPT, preferences.autoAcceptFromContacts)
            putString(KEY_VISIBILITY, preferences.deviceVisibility.name)
            putBoolean(KEY_SHOW_QUICK_SHARE, preferences.showQuickShareOption)
            apply()
        }
    }
    
    /**
     * Get recent shares
     */
    fun getRecentShares(): List<RecentShare> {
        val json = prefs.getString(KEY_RECENT_SHARES, null) ?: return emptyList()
        return try {
            val gson = com.google.gson.Gson()
            val type = object : com.google.gson.reflect.TypeToken<List<RecentShare>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Add a recent share
     */
    fun addRecentShare(share: RecentShare) {
        val current = getRecentShares().toMutableList()
        current.add(0, share)
        
        // Keep only last 10 shares
        val trimmed = current.take(10)
        
        // Save to preferences using Gson
        val gson = com.google.gson.Gson()
        prefs.edit().putString(KEY_RECENT_SHARES, gson.toJson(trimmed)).apply()
    }
    
    /**
     * Clear recent shares
     */
    fun clearRecentShares() {
        prefs.edit().remove(KEY_RECENT_SHARES).apply()
    }
    
    /**
     * Get MIME type from file name
     */
    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".pdf", true) -> "application/pdf"
            fileName.endsWith(".docx", true) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            fileName.endsWith(".xlsx", true) -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            fileName.endsWith(".pptx", true) -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            fileName.endsWith(".txt", true) -> "text/plain"
            fileName.endsWith(".md", true) -> "text/markdown"
            fileName.endsWith(".jpg", true) || fileName.endsWith(".jpeg", true) -> "image/jpeg"
            fileName.endsWith(".png", true) -> "image/png"
            else -> "*/*"
        }
    }
    
    /**
     * Check if quick share is enabled
     */
    fun isQuickShareEnabled(): Boolean {
        return prefs.getBoolean(KEY_SHOW_QUICK_SHARE, true)
    }
    
    /**
     * Enable or disable quick share option
     */
    fun setQuickShareEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_QUICK_SHARE, enabled).apply()
    }
    
    companion object {
        private const val PREFS_NAME = "nearby_share_prefs"
        private const val KEY_AUTO_ACCEPT = "auto_accept_contacts"
        private const val KEY_VISIBILITY = "device_visibility"
        private const val KEY_SHOW_QUICK_SHARE = "show_quick_share"
        private const val KEY_RECENT_SHARES = "recent_shares"
    }
}
