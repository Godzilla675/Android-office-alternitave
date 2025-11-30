package com.officesuite.app.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.officesuite.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Utility class to check for app updates from GitHub releases.
 */
object UpdateChecker {
    
    private const val PREFS_NAME = "update_prefs"
    private const val KEY_LAST_CHECK = "last_check_time"
    private const val KEY_DISMISSED_VERSION = "dismissed_version"
    private const val KEY_AUTO_CHECK_ENABLED = "auto_check_enabled"
    private const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours
    
    data class ReleaseInfo(
        @SerializedName("tag_name") val tagName: String,
        @SerializedName("name") val name: String,
        @SerializedName("body") val body: String,
        @SerializedName("html_url") val htmlUrl: String,
        @SerializedName("published_at") val publishedAt: String = "",
        @SerializedName("assets") val assets: List<Asset>
    )
    
    data class Asset(
        @SerializedName("name") val name: String,
        @SerializedName("browser_download_url") val downloadUrl: String,
        @SerializedName("size") val size: Long
    )
    
    data class UpdateInfo(
        val currentVersion: String,
        val latestVersion: String,
        val releaseNotes: String,
        val downloadUrl: String,
        val releaseName: String,
        val htmlUrl: String
    )
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Check if auto-update check is enabled
     */
    fun isAutoCheckEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUTO_CHECK_ENABLED, true)
    }
    
    /**
     * Set auto-update check preference
     */
    fun setAutoCheckEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUTO_CHECK_ENABLED, enabled).apply()
    }
    
    /**
     * Check if enough time has passed since the last update check
     */
    fun shouldCheckForUpdate(context: Context): Boolean {
        if (!isAutoCheckEnabled(context)) return false
        
        val lastCheck = getPrefs(context).getLong(KEY_LAST_CHECK, 0)
        return System.currentTimeMillis() - lastCheck >= CHECK_INTERVAL_MS
    }
    
    /**
     * Mark that an update check was performed
     */
    fun markUpdateChecked(context: Context) {
        getPrefs(context).edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
    }
    
    /**
     * Dismiss a specific version (user chose to skip this update)
     */
    fun dismissVersion(context: Context, version: String) {
        getPrefs(context).edit().putString(KEY_DISMISSED_VERSION, version).apply()
    }
    
    /**
     * Check if a version was dismissed by the user
     */
    fun isVersionDismissed(context: Context, version: String): Boolean {
        return getPrefs(context).getString(KEY_DISMISSED_VERSION, null) == version
    }
    
    /**
     * Check for updates from GitHub releases API.
     * Returns UpdateInfo if an update is available, null otherwise.
     */
    suspend fun checkForUpdate(context: Context, forceCheck: Boolean = false): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val owner = BuildConfig.GITHUB_REPO_OWNER
                val repo = BuildConfig.GITHUB_REPO_NAME
                val url = URL("https://api.github.com/repos/$owner/$repo/releases/latest")
                
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val release = Gson().fromJson(response, ReleaseInfo::class.java)
                    
                    val latestVersion = release.tagName.removePrefix("v")
                    val currentVersion = BuildConfig.VERSION_NAME
                    
                    // Check if update is available
                    if (isNewerVersion(latestVersion, currentVersion)) {
                        // Check if this version was dismissed
                        if (!forceCheck && isVersionDismissed(context, latestVersion)) {
                            return@withContext null
                        }
                        
                        // Find APK download URL
                        val apkAsset = release.assets.find { it.name.endsWith(".apk") }
                        val downloadUrl = apkAsset?.downloadUrl ?: release.htmlUrl
                        
                        return@withContext UpdateInfo(
                            currentVersion = currentVersion,
                            latestVersion = latestVersion,
                            releaseNotes = release.body,
                            downloadUrl = downloadUrl,
                            releaseName = release.name,
                            htmlUrl = release.htmlUrl
                        )
                    }
                }
                null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                connection?.disconnect()
            }
        }
    }
    
    /**
     * Compare two version strings.
     * Returns true if version1 is newer than version2.
     * Handles versions with pre-release suffixes like "1.0.0-alpha" or "2.0.0-beta.1"
     */
    fun isNewerVersion(version1: String, version2: String): Boolean {
        try {
            // Remove pre-release suffix for comparison (e.g., -alpha, -beta)
            val cleanVersion1 = version1.split("-").first()
            val cleanVersion2 = version2.split("-").first()
            
            val parts1 = cleanVersion1.split(".").map { it.toIntOrNull() ?: 0 }
            val parts2 = cleanVersion2.split(".").map { it.toIntOrNull() ?: 0 }
            
            val maxLength = maxOf(parts1.size, parts2.size)
            
            for (i in 0 until maxLength) {
                val v1 = parts1.getOrElse(i) { 0 }
                val v2 = parts2.getOrElse(i) { 0 }
                
                if (v1 > v2) return true
                if (v1 < v2) return false
            }
            
            // If versions are numerically equal, a release version is newer than pre-release
            // e.g., 1.0.0 is newer than 1.0.0-beta
            val hasPreRelease1 = version1.contains("-")
            val hasPreRelease2 = version2.contains("-")
            
            if (!hasPreRelease1 && hasPreRelease2) return true
            if (hasPreRelease1 && !hasPreRelease2) return false
            
            return false // Versions are equal
        } catch (e: Exception) {
            return false
        }
    }
    
    /**
     * Fetch all releases from GitHub releases API.
     * Returns a list of ReleaseInfo objects.
     */
    suspend fun getAllReleases(): List<ReleaseInfo> {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val owner = BuildConfig.GITHUB_REPO_OWNER
                val repo = BuildConfig.GITHUB_REPO_NAME
                val url = URL("https://api.github.com/repos/$owner/$repo/releases")
                
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val listType = object : TypeToken<List<ReleaseInfo>>() {}.type
                    return@withContext Gson().fromJson<List<ReleaseInfo>>(response, listType)
                }
                return@withContext emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext emptyList()
            } finally {
                connection?.disconnect()
            }
        }
    }
}
