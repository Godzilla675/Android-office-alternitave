package com.officesuite.app.platform

import com.officesuite.app.R
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for QuickSettingsManager class.
 */
class QuickSettingsManagerTest {
    
    @Test
    fun `TileType enum has correct values`() {
        val types = QuickSettingsManager.TileType.entries
        assertEquals(3, types.size)
        assertTrue(types.contains(QuickSettingsManager.TileType.SCANNER))
        assertTrue(types.contains(QuickSettingsManager.TileType.CREATE))
        assertTrue(types.contains(QuickSettingsManager.TileType.CONVERT))
    }
    
    @Test
    fun `TileType SCANNER has correct properties`() {
        val tile = QuickSettingsManager.TileType.SCANNER
        assertEquals("scanner_tile", tile.id)
        assertEquals(R.string.scan_document, tile.titleRes)
        assertEquals(R.string.shortcut_scan_long, tile.descriptionRes)
    }
    
    @Test
    fun `TileType CREATE has correct properties`() {
        val tile = QuickSettingsManager.TileType.CREATE
        assertEquals("create_tile", tile.id)
        assertEquals(R.string.create_new, tile.titleRes)
        assertEquals(R.string.shortcut_create_long, tile.descriptionRes)
    }
    
    @Test
    fun `TileType CONVERT has correct properties`() {
        val tile = QuickSettingsManager.TileType.CONVERT
        assertEquals("convert_tile", tile.id)
        assertEquals(R.string.convert, tile.titleRes)
        assertEquals(R.string.shortcut_convert_long, tile.descriptionRes)
    }
    
    @Test
    fun `TileConfig data class holds values correctly`() {
        val config = QuickSettingsManager.TileConfig(
            type = QuickSettingsManager.TileType.SCANNER,
            isEnabled = true,
            customLabel = "Quick Scan"
        )
        
        assertEquals(QuickSettingsManager.TileType.SCANNER, config.type)
        assertTrue(config.isEnabled)
        assertEquals("Quick Scan", config.customLabel)
    }
    
    @Test
    fun `TileConfig with null customLabel`() {
        val config = QuickSettingsManager.TileConfig(
            type = QuickSettingsManager.TileType.CREATE,
            isEnabled = false,
            customLabel = null
        )
        
        assertEquals(QuickSettingsManager.TileType.CREATE, config.type)
        assertFalse(config.isEnabled)
        assertNull(config.customLabel)
    }
    
    @Test
    fun `All TileTypes have unique IDs`() {
        val types = QuickSettingsManager.TileType.entries
        val ids = types.map { it.id }
        val uniqueIds = ids.toSet()
        assertEquals(ids.size, uniqueIds.size)
    }
    
    @Test
    fun `All TileTypes have non-zero resource IDs`() {
        val types = QuickSettingsManager.TileType.entries
        types.forEach { type ->
            assertTrue("Tile ${type.id} has zero titleRes", type.titleRes != 0)
            assertTrue("Tile ${type.id} has zero descriptionRes", type.descriptionRes != 0)
        }
    }
}
