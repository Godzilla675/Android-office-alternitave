package com.officesuite.app.cloud

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manager for coordinating cloud storage operations across multiple providers.
 * Includes local storage provider by default.
 */
class CloudStorageManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "cloud_storage_prefs"
        private const val KEY_DEFAULT_PROVIDER = "default_provider"
        private const val KEY_AUTO_SYNC_ENABLED = "auto_sync_enabled"
        private const val KEY_SYNC_ON_WIFI_ONLY = "sync_on_wifi_only"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
    }

    /**
     * Supported cloud storage providers
     */
    enum class ProviderType {
        LOCAL,
        GOOGLE_DRIVE,
        DROPBOX,
        ONEDRIVE
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val providers = mutableMapOf<ProviderType, CloudStorageProvider>()
    
    // Local storage provider is always available
    private val localStorageProvider = LocalStorageProvider(context)
    
    init {
        // Register local storage provider by default
        providers[ProviderType.LOCAL] = localStorageProvider
    }

    /**
     * Register a cloud storage provider
     */
    fun registerProvider(type: ProviderType, provider: CloudStorageProvider) {
        providers[type] = provider
    }

    /**
     * Get a registered provider
     */
    fun getProvider(type: ProviderType): CloudStorageProvider? {
        return providers[type]
    }

    /**
     * Get all registered providers
     */
    fun getAllProviders(): Map<ProviderType, CloudStorageProvider> {
        return providers.toMap()
    }

    /**
     * Get the default cloud provider
     */
    fun getDefaultProvider(): ProviderType? {
        val name = prefs.getString(KEY_DEFAULT_PROVIDER, null) ?: return null
        return try {
            ProviderType.valueOf(name)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /**
     * Set the default cloud provider
     */
    fun setDefaultProvider(type: ProviderType) {
        prefs.edit().putString(KEY_DEFAULT_PROVIDER, type.name).apply()
    }

    /**
     * Check if auto-sync is enabled
     */
    fun isAutoSyncEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_SYNC_ENABLED, false)
    }

    /**
     * Enable or disable auto-sync
     */
    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SYNC_ENABLED, enabled).apply()
    }

    /**
     * Check if sync is limited to WiFi only
     */
    fun isSyncOnWifiOnly(): Boolean {
        return prefs.getBoolean(KEY_SYNC_ON_WIFI_ONLY, true)
    }

    /**
     * Set whether to sync on WiFi only
     */
    fun setSyncOnWifiOnly(wifiOnly: Boolean) {
        prefs.edit().putBoolean(KEY_SYNC_ON_WIFI_ONLY, wifiOnly).apply()
    }

    /**
     * Check if any provider is authenticated
     */
    suspend fun hasAuthenticatedProvider(): Boolean {
        return providers.values.any { it.isAuthenticated() }
    }

    /**
     * Get list of authenticated providers
     */
    suspend fun getAuthenticatedProviders(): List<ProviderType> {
        return providers.filter { it.value.isAuthenticated() }.keys.toList()
    }

    /**
     * Upload file to default provider
     */
    suspend fun uploadToDefault(
        localFile: File,
        folderId: String? = null
    ): CloudResult<CloudFile> {
        val defaultType = getDefaultProvider()
            ?: return CloudResult.Error("No default provider set")
        
        val provider = providers[defaultType]
            ?: return CloudResult.Error("Default provider not registered")
        
        return provider.uploadFile(localFile, folderId)
    }

    /**
     * Download file from a provider
     */
    suspend fun downloadFile(
        providerType: ProviderType,
        fileId: String,
        destinationFile: File
    ): CloudResult<File> {
        val provider = providers[providerType]
            ?: return CloudResult.Error("Provider not registered")
        
        return provider.downloadFile(fileId, destinationFile)
    }

    /**
     * Search files across all authenticated providers
     */
    suspend fun searchAllProviders(query: String): Map<ProviderType, CloudResult<List<CloudFile>>> {
        val results = mutableMapOf<ProviderType, CloudResult<List<CloudFile>>>()
        
        providers.forEach { (type, provider) ->
            if (provider.isAuthenticated()) {
                results[type] = provider.searchFiles(query)
            }
        }
        
        return results
    }
    
    /**
     * Get the last sync time
     */
    fun getLastSyncTime(): Long {
        return prefs.getLong(KEY_LAST_SYNC_TIME, 0)
    }
    
    /**
     * Set the last sync time
     */
    fun setLastSyncTime(time: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC_TIME, time).apply()
    }
    
    /**
     * Check if device is connected to WiFi
     */
    fun isConnectedToWifi(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
    
    /**
     * Check if sync should proceed based on settings
     */
    fun canSync(): Boolean {
        if (!isAutoSyncEnabled()) return true // Manual sync always allowed
        if (isSyncOnWifiOnly() && !isConnectedToWifi()) return false
        return true
    }
    
    /**
     * Sync local files to cloud
     * Returns the number of files synced
     */
    suspend fun syncToCloud(
        localFiles: List<File>,
        providerType: ProviderType = getDefaultProvider() ?: ProviderType.LOCAL
    ): SyncResult = withContext(Dispatchers.IO) {
        if (!canSync() && providerType != ProviderType.LOCAL) {
            return@withContext SyncResult(
                success = false,
                syncedCount = 0,
                errorMessage = "WiFi required for sync"
            )
        }
        
        val provider = providers[providerType]
            ?: return@withContext SyncResult(
                success = false,
                syncedCount = 0,
                errorMessage = "Provider not registered"
            )
        
        var syncedCount = 0
        var errorMessage: String? = null
        
        for (file in localFiles) {
            try {
                val result = provider.uploadFile(file)
                if (result.isSuccess) {
                    syncedCount++
                }
            } catch (e: Exception) {
                errorMessage = e.message
            }
        }
        
        if (syncedCount > 0) {
            setLastSyncTime(System.currentTimeMillis())
        }
        
        SyncResult(
            success = errorMessage == null,
            syncedCount = syncedCount,
            errorMessage = errorMessage
        )
    }
    
    /**
     * Get local storage provider
     */
    fun getLocalStorageProvider(): LocalStorageProvider = localStorageProvider
    
    /**
     * List all files from local cloud-like storage
     */
    suspend fun listLocalFiles(): CloudResult<List<CloudFile>> {
        return localStorageProvider.listFiles()
    }
    
    data class SyncResult(
        val success: Boolean,
        val syncedCount: Int,
        val errorMessage: String? = null
    )
}
