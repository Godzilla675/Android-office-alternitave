package com.officesuite.app.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.officesuite.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Utility class to check for app updates from GitHub releases.
 * Includes rate limit handling with exponential backoff retry logic.
 */
object UpdateChecker {
    
    private const val PREFS_NAME = "update_prefs"
    private const val KEY_LAST_CHECK = "last_check_time"
    private const val KEY_DISMISSED_VERSION = "dismissed_version"
    private const val KEY_AUTO_CHECK_ENABLED = "auto_check_enabled"
    private const val KEY_RATE_LIMIT_RESET_TIME = "rate_limit_reset_time"
    private const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours
    
    // Rate limiting constants
    private const val MAX_RETRIES = 3
    private const val INITIAL_BACKOFF_MS = 1000L
    private const val MAX_BACKOFF_MS = 30000L
    
    // HTTP status codes for rate limiting
    private const val HTTP_TOO_MANY_REQUESTS = 429
    private const val HTTP_FORBIDDEN = 403
    
    // GitHub rate limit headers
    private const val HEADER_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining"
    private const val HEADER_RATE_LIMIT_RESET = "X-RateLimit-Reset"
    private const val HEADER_RETRY_AFTER = "Retry-After"
    
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
    
    /**
     * Rate limit information from GitHub API
     */
    data class RateLimitInfo(
        val remaining: Int,
        val resetTimeSeconds: Long,
        val isRateLimited: Boolean
    )
    
    /**
     * Result wrapper for API calls that may be rate limited
     */
    sealed class ApiResult<T> {
        data class Success<T>(val data: T) : ApiResult<T>()
        data class RateLimited<T>(val resetTimeSeconds: Long, val message: String) : ApiResult<T>()
        data class Error<T>(val message: String, val exception: Exception? = null) : ApiResult<T>()
    }
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Check if currently rate limited based on stored reset time
     */
    fun isRateLimited(context: Context): Boolean {
        val resetTime = getPrefs(context).getLong(KEY_RATE_LIMIT_RESET_TIME, 0)
        return System.currentTimeMillis() / 1000 < resetTime
    }
    
    /**
     * Get the rate limit reset time in seconds since epoch
     */
    fun getRateLimitResetTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_RATE_LIMIT_RESET_TIME, 0)
    }
    
    /**
     * Store the rate limit reset time
     */
    private fun setRateLimitResetTime(context: Context, resetTimeSeconds: Long) {
        getPrefs(context).edit().putLong(KEY_RATE_LIMIT_RESET_TIME, resetTimeSeconds).apply()
    }
    
    /**
     * Clear the rate limit state
     */
    fun clearRateLimitState(context: Context) {
        getPrefs(context).edit().remove(KEY_RATE_LIMIT_RESET_TIME).apply()
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
     * Includes rate limit handling with exponential backoff.
     */
    suspend fun checkForUpdate(context: Context, forceCheck: Boolean = false): UpdateInfo? {
        // Check if we're currently rate limited
        if (isRateLimited(context)) {
            val resetTime = getRateLimitResetTime(context)
            val waitSeconds = resetTime - (System.currentTimeMillis() / 1000)
            if (waitSeconds > 0) {
                // Still rate limited, skip the check
                return null
            } else {
                // Rate limit expired, clear the state
                clearRateLimitState(context)
            }
        }
        
        return withContext(Dispatchers.IO) {
            var lastException: Exception? = null
            var currentBackoff = INITIAL_BACKOFF_MS
            
            repeat(MAX_RETRIES) { attempt ->
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
                    
                    val responseCode = connection.responseCode
                    
                    // Check for rate limiting
                    if (responseCode == HTTP_TOO_MANY_REQUESTS || 
                        (responseCode == HTTP_FORBIDDEN && isRateLimitResponse(connection))) {
                        
                        val retryAfter = getRetryAfterSeconds(connection)
                        val resetTime = getRateLimitResetFromHeaders(connection)
                        
                        // Store the rate limit reset time
                        if (resetTime > 0) {
                            setRateLimitResetTime(context, resetTime)
                        }
                        
                        // If we have retries left and the wait is reasonable, retry
                        if (attempt < MAX_RETRIES - 1 && retryAfter <= MAX_BACKOFF_MS / 1000) {
                            val waitTime = if (retryAfter > 0) retryAfter * 1000 else currentBackoff
                            delay(waitTime)
                            currentBackoff = (currentBackoff * 2).coerceAtMost(MAX_BACKOFF_MS)
                            connection.disconnect()
                            return@repeat // Continue to next retry
                        }
                        
                        // Rate limited and can't retry
                        return@withContext null
                    }
                    
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        // Clear any previous rate limit state on successful request
                        clearRateLimitState(context)
                        
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
                        return@withContext null
                    }
                    
                    // For other errors, retry with backoff
                    if (attempt < MAX_RETRIES - 1) {
                        delay(currentBackoff)
                        currentBackoff = (currentBackoff * 2).coerceAtMost(MAX_BACKOFF_MS)
                    }
                    
                } catch (e: Exception) {
                    lastException = e
                    if (attempt < MAX_RETRIES - 1) {
                        delay(currentBackoff)
                        currentBackoff = (currentBackoff * 2).coerceAtMost(MAX_BACKOFF_MS)
                    }
                } finally {
                    connection?.disconnect()
                }
            }
            
            lastException?.printStackTrace()
            null
        }
    }
    
    /**
     * Check if a 403 response is due to rate limiting
     */
    private fun isRateLimitResponse(connection: HttpURLConnection): Boolean {
        val remaining = connection.getHeaderField(HEADER_RATE_LIMIT_REMAINING)
        return remaining == "0"
    }
    
    /**
     * Get the Retry-After header value in seconds
     */
    private fun getRetryAfterSeconds(connection: HttpURLConnection): Long {
        return connection.getHeaderField(HEADER_RETRY_AFTER)?.toLongOrNull() ?: 0
    }
    
    /**
     * Get the rate limit reset time from headers
     */
    private fun getRateLimitResetFromHeaders(connection: HttpURLConnection): Long {
        return connection.getHeaderField(HEADER_RATE_LIMIT_RESET)?.toLongOrNull() ?: 0
    }
    
    /**
     * Get rate limit information from a connection
     */
    fun getRateLimitInfo(connection: HttpURLConnection): RateLimitInfo {
        val remaining = connection.getHeaderField(HEADER_RATE_LIMIT_REMAINING)?.toIntOrNull() ?: -1
        val resetTime = connection.getHeaderField(HEADER_RATE_LIMIT_RESET)?.toLongOrNull() ?: 0
        val isRateLimited = remaining == 0
        return RateLimitInfo(remaining, resetTime, isRateLimited)
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
     * Includes rate limit handling with exponential backoff.
     */
    suspend fun getAllReleases(): List<ReleaseInfo> {
        return withContext(Dispatchers.IO) {
            var lastException: Exception? = null
            var currentBackoff = INITIAL_BACKOFF_MS
            
            repeat(MAX_RETRIES) { attempt ->
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
                    
                    val responseCode = connection.responseCode
                    
                    // Check for rate limiting
                    if (responseCode == HTTP_TOO_MANY_REQUESTS || 
                        (responseCode == HTTP_FORBIDDEN && isRateLimitResponse(connection))) {
                        
                        val retryAfter = getRetryAfterSeconds(connection)
                        
                        // If we have retries left and the wait is reasonable, retry
                        if (attempt < MAX_RETRIES - 1 && retryAfter <= MAX_BACKOFF_MS / 1000) {
                            val waitTime = if (retryAfter > 0) retryAfter * 1000 else currentBackoff
                            delay(waitTime)
                            currentBackoff = (currentBackoff * 2).coerceAtMost(MAX_BACKOFF_MS)
                            connection.disconnect()
                            return@repeat // Continue to next retry
                        }
                        
                        // Rate limited and can't retry
                        return@withContext emptyList()
                    }
                    
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        val listType = object : TypeToken<List<ReleaseInfo>>() {}.type
                        return@withContext Gson().fromJson<List<ReleaseInfo>>(response, listType)
                    }
                    
                    // For other errors, retry with backoff
                    if (attempt < MAX_RETRIES - 1) {
                        delay(currentBackoff)
                        currentBackoff = (currentBackoff * 2).coerceAtMost(MAX_BACKOFF_MS)
                    }
                    
                } catch (e: Exception) {
                    lastException = e
                    if (attempt < MAX_RETRIES - 1) {
                        delay(currentBackoff)
                        currentBackoff = (currentBackoff * 2).coerceAtMost(MAX_BACKOFF_MS)
                    }
                } finally {
                    connection?.disconnect()
                }
            }
            
            lastException?.printStackTrace()
            emptyList()
        }
    }
}
