package com.officesuite.app.platform

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for NearbyShareManager class.
 */
class NearbyShareManagerTest {
    
    @Test
    fun `ShareTarget data class holds values correctly`() {
        val target = NearbyShareManager.ShareTarget(
            name = "John's Pixel",
            deviceType = NearbyShareManager.DeviceType.PHONE,
            isRecent = true
        )
        
        assertEquals("John's Pixel", target.name)
        assertEquals(NearbyShareManager.DeviceType.PHONE, target.deviceType)
        assertTrue(target.isRecent)
    }
    
    @Test
    fun `DeviceType enum has correct values`() {
        val types = NearbyShareManager.DeviceType.entries
        assertEquals(5, types.size)
        assertTrue(types.contains(NearbyShareManager.DeviceType.PHONE))
        assertTrue(types.contains(NearbyShareManager.DeviceType.TABLET))
        assertTrue(types.contains(NearbyShareManager.DeviceType.COMPUTER))
        assertTrue(types.contains(NearbyShareManager.DeviceType.TV))
        assertTrue(types.contains(NearbyShareManager.DeviceType.UNKNOWN))
    }
    
    @Test
    fun `ShareResult sealed class has correct subclasses`() {
        val success = NearbyShareManager.ShareResult.Success
        val error = NearbyShareManager.ShareResult.Error("Test error")
        val cancelled = NearbyShareManager.ShareResult.Cancelled
        val unavailable = NearbyShareManager.ShareResult.NearbyShareUnavailable
        
        assertTrue(success is NearbyShareManager.ShareResult)
        assertTrue(error is NearbyShareManager.ShareResult)
        assertTrue(cancelled is NearbyShareManager.ShareResult)
        assertTrue(unavailable is NearbyShareManager.ShareResult)
        
        assertEquals("Test error", (error as NearbyShareManager.ShareResult.Error).message)
    }
    
    @Test
    fun `SharePreferences data class holds values correctly`() {
        val prefs = NearbyShareManager.SharePreferences(
            autoAcceptFromContacts = true,
            deviceVisibility = NearbyShareManager.DeviceVisibility.EVERYONE,
            showQuickShareOption = true
        )
        
        assertTrue(prefs.autoAcceptFromContacts)
        assertEquals(NearbyShareManager.DeviceVisibility.EVERYONE, prefs.deviceVisibility)
        assertTrue(prefs.showQuickShareOption)
    }
    
    @Test
    fun `SharePreferences default values are correct`() {
        val defaultPrefs = NearbyShareManager.SharePreferences()
        
        assertFalse(defaultPrefs.autoAcceptFromContacts)
        assertEquals(NearbyShareManager.DeviceVisibility.CONTACTS_ONLY, defaultPrefs.deviceVisibility)
        assertTrue(defaultPrefs.showQuickShareOption)
    }
    
    @Test
    fun `DeviceVisibility enum has correct titles`() {
        assertEquals("Everyone", NearbyShareManager.DeviceVisibility.EVERYONE.title)
        assertEquals("Contacts only", NearbyShareManager.DeviceVisibility.CONTACTS_ONLY.title)
        assertEquals("Hidden", NearbyShareManager.DeviceVisibility.HIDDEN.title)
    }
    
    @Test
    fun `RecentShare data class holds values correctly`() {
        val share = NearbyShareManager.RecentShare(
            fileName = "document.pdf",
            targetDevice = "John's Pixel",
            timestamp = 1704067200000L,
            wasSuccessful = true
        )
        
        assertEquals("document.pdf", share.fileName)
        assertEquals("John's Pixel", share.targetDevice)
        assertEquals(1704067200000L, share.timestamp)
        assertTrue(share.wasSuccessful)
    }
    
    @Test
    fun `DeviceVisibility enum has all expected values`() {
        val visibilities = NearbyShareManager.DeviceVisibility.entries
        assertEquals(3, visibilities.size)
        assertTrue(visibilities.contains(NearbyShareManager.DeviceVisibility.EVERYONE))
        assertTrue(visibilities.contains(NearbyShareManager.DeviceVisibility.CONTACTS_ONLY))
        assertTrue(visibilities.contains(NearbyShareManager.DeviceVisibility.HIDDEN))
    }
}
