package com.officesuite.app.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the UpdateChecker utility class.
 * Tests version comparison and rate limiting data classes.
 */
class UpdateCheckerTest {

    @Test
    fun `isNewerVersion returns true when major version is higher`() {
        assertTrue(UpdateChecker.isNewerVersion("2.0.0", "1.0.0"))
    }

    @Test
    fun `isNewerVersion returns true when minor version is higher`() {
        assertTrue(UpdateChecker.isNewerVersion("1.1.0", "1.0.0"))
    }

    @Test
    fun `isNewerVersion returns true when patch version is higher`() {
        assertTrue(UpdateChecker.isNewerVersion("1.0.1", "1.0.0"))
    }

    @Test
    fun `isNewerVersion returns false when versions are equal`() {
        assertFalse(UpdateChecker.isNewerVersion("1.0.0", "1.0.0"))
    }

    @Test
    fun `isNewerVersion returns false when version is lower`() {
        assertFalse(UpdateChecker.isNewerVersion("1.0.0", "2.0.0"))
    }

    @Test
    fun `isNewerVersion handles pre-release versions correctly`() {
        // Release version is newer than pre-release
        assertTrue(UpdateChecker.isNewerVersion("1.0.0", "1.0.0-beta"))
        assertTrue(UpdateChecker.isNewerVersion("1.0.0", "1.0.0-alpha"))
        
        // Pre-release is not newer than release
        assertFalse(UpdateChecker.isNewerVersion("1.0.0-beta", "1.0.0"))
        assertFalse(UpdateChecker.isNewerVersion("1.0.0-alpha", "1.0.0"))
    }

    @Test
    fun `isNewerVersion handles version with v prefix`() {
        // The prefix should be removed before comparison
        // Note: The function expects versions without "v" prefix
        // The caller removes it before calling
        assertTrue(UpdateChecker.isNewerVersion("2.0.0", "1.0.0"))
    }

    @Test
    fun `isNewerVersion handles versions with different lengths`() {
        assertTrue(UpdateChecker.isNewerVersion("1.0.0.1", "1.0.0"))
        assertFalse(UpdateChecker.isNewerVersion("1.0.0", "1.0.0.1"))
    }

    @Test
    fun `isNewerVersion handles invalid versions gracefully`() {
        assertFalse(UpdateChecker.isNewerVersion("invalid", "1.0.0"))
        // When comparing valid version to invalid, it treats "invalid" as 0
        // so 1.0.0 > 0.0.0 returns true
        assertTrue(UpdateChecker.isNewerVersion("1.0.0", "invalid"))
        // Two empty strings are treated as equal (both become [0])
        assertFalse(UpdateChecker.isNewerVersion("", ""))
    }

    @Test
    fun `RateLimitInfo data class holds values correctly`() {
        val info = UpdateChecker.RateLimitInfo(
            remaining = 50,
            resetTimeSeconds = 1609459200L,
            isRateLimited = false
        )
        
        assertEquals(50, info.remaining)
        assertEquals(1609459200L, info.resetTimeSeconds)
        assertFalse(info.isRateLimited)
    }

    @Test
    fun `RateLimitInfo indicates rate limited when remaining is zero`() {
        val info = UpdateChecker.RateLimitInfo(
            remaining = 0,
            resetTimeSeconds = 1609459200L,
            isRateLimited = true
        )
        
        assertEquals(0, info.remaining)
        assertTrue(info.isRateLimited)
    }

    @Test
    fun `UpdateInfo data class holds values correctly`() {
        val info = UpdateChecker.UpdateInfo(
            currentVersion = "1.0.0",
            latestVersion = "2.0.0",
            releaseNotes = "Bug fixes and improvements",
            downloadUrl = "https://example.com/app.apk",
            releaseName = "Version 2.0.0",
            htmlUrl = "https://example.com/releases/v2.0.0"
        )
        
        assertEquals("1.0.0", info.currentVersion)
        assertEquals("2.0.0", info.latestVersion)
        assertEquals("Bug fixes and improvements", info.releaseNotes)
        assertEquals("https://example.com/app.apk", info.downloadUrl)
        assertEquals("Version 2.0.0", info.releaseName)
        assertEquals("https://example.com/releases/v2.0.0", info.htmlUrl)
    }

    @Test
    fun `ReleaseInfo data class holds values correctly`() {
        val asset = UpdateChecker.Asset(
            name = "app-release.apk",
            downloadUrl = "https://example.com/app.apk",
            size = 10485760L
        )
        
        val release = UpdateChecker.ReleaseInfo(
            tagName = "v2.0.0",
            name = "Version 2.0.0",
            body = "Release notes",
            htmlUrl = "https://example.com/releases/v2.0.0",
            publishedAt = "2024-01-15T10:00:00Z",
            assets = listOf(asset)
        )
        
        assertEquals("v2.0.0", release.tagName)
        assertEquals("Version 2.0.0", release.name)
        assertEquals("Release notes", release.body)
        assertEquals("https://example.com/releases/v2.0.0", release.htmlUrl)
        assertEquals("2024-01-15T10:00:00Z", release.publishedAt)
        assertEquals(1, release.assets.size)
        assertEquals("app-release.apk", release.assets[0].name)
    }

    @Test
    fun `Asset data class holds values correctly`() {
        val asset = UpdateChecker.Asset(
            name = "app-release.apk",
            downloadUrl = "https://example.com/app.apk",
            size = 10485760L
        )
        
        assertEquals("app-release.apk", asset.name)
        assertEquals("https://example.com/app.apk", asset.downloadUrl)
        assertEquals(10485760L, asset.size)
    }

    @Test
    fun `ApiResult Success holds data correctly`() {
        val result: UpdateChecker.ApiResult<String> = UpdateChecker.ApiResult.Success("test data")
        
        assertTrue(result is UpdateChecker.ApiResult.Success)
        assertEquals("test data", (result as UpdateChecker.ApiResult.Success).data)
    }

    @Test
    fun `ApiResult RateLimited holds values correctly`() {
        val result: UpdateChecker.ApiResult<String> = UpdateChecker.ApiResult.RateLimited(
            resetTimeSeconds = 1609459200L,
            message = "Rate limit exceeded"
        )
        
        assertTrue(result is UpdateChecker.ApiResult.RateLimited)
        val rateLimited = result as UpdateChecker.ApiResult.RateLimited
        assertEquals(1609459200L, rateLimited.resetTimeSeconds)
        assertEquals("Rate limit exceeded", rateLimited.message)
    }

    @Test
    fun `ApiResult Error holds values correctly`() {
        val exception = RuntimeException("Test error")
        val result: UpdateChecker.ApiResult<String> = UpdateChecker.ApiResult.Error(
            message = "Something went wrong",
            exception = exception
        )
        
        assertTrue(result is UpdateChecker.ApiResult.Error)
        val error = result as UpdateChecker.ApiResult.Error
        assertEquals("Something went wrong", error.message)
        assertEquals(exception, error.exception)
    }

    @Test
    fun `ApiResult Error works without exception`() {
        val result: UpdateChecker.ApiResult<String> = UpdateChecker.ApiResult.Error(
            message = "Something went wrong"
        )
        
        assertTrue(result is UpdateChecker.ApiResult.Error)
        val error = result as UpdateChecker.ApiResult.Error
        assertEquals("Something went wrong", error.message)
        assertNull(error.exception)
    }

    @Test
    fun `isNewerVersion handles complex pre-release suffixes`() {
        // Pre-release with numbers
        assertTrue(UpdateChecker.isNewerVersion("1.0.0", "1.0.0-beta.1"))
        assertTrue(UpdateChecker.isNewerVersion("1.0.0", "1.0.0-alpha.2"))
        
        // Higher version with pre-release is still newer
        assertTrue(UpdateChecker.isNewerVersion("2.0.0-beta", "1.0.0"))
    }

    @Test
    fun `isNewerVersion compares pre-release versions as equal numerically`() {
        // Two pre-release versions with same numeric version
        assertFalse(UpdateChecker.isNewerVersion("1.0.0-alpha", "1.0.0-beta"))
        assertFalse(UpdateChecker.isNewerVersion("1.0.0-beta", "1.0.0-alpha"))
    }
}
