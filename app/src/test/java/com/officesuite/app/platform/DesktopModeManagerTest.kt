package com.officesuite.app.platform

import android.view.KeyEvent
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for DesktopModeManager class.
 */
class DesktopModeManagerTest {
    
    @Test
    fun `DesktopModeState data class holds values correctly`() {
        val state = DesktopModeManager.DesktopModeState(
            isDesktopMode = true,
            isDexMode = true,
            isChromeOS = false,
            hasPhysicalKeyboard = true,
            hasMouseConnected = true,
            screenSize = DesktopModeManager.ScreenSize.EXPANDED
        )
        
        assertTrue(state.isDesktopMode)
        assertTrue(state.isDexMode)
        assertFalse(state.isChromeOS)
        assertTrue(state.hasPhysicalKeyboard)
        assertTrue(state.hasMouseConnected)
        assertEquals(DesktopModeManager.ScreenSize.EXPANDED, state.screenSize)
    }
    
    @Test
    fun `ScreenSize enum has correct values`() {
        val sizes = DesktopModeManager.ScreenSize.entries
        assertEquals(3, sizes.size)
        assertTrue(sizes.contains(DesktopModeManager.ScreenSize.COMPACT))
        assertTrue(sizes.contains(DesktopModeManager.ScreenSize.MEDIUM))
        assertTrue(sizes.contains(DesktopModeManager.ScreenSize.EXPANDED))
    }
    
    @Test
    fun `KeyboardShortcut data class holds values correctly`() {
        val shortcut = DesktopModeManager.KeyboardShortcut(
            keyCode = KeyEvent.KEYCODE_S,
            modifiers = KeyEvent.META_CTRL_ON,
            actionId = "save",
            description = "Save",
            isCustomizable = true
        )
        
        assertEquals(KeyEvent.KEYCODE_S, shortcut.keyCode)
        assertEquals(KeyEvent.META_CTRL_ON, shortcut.modifiers)
        assertEquals("save", shortcut.actionId)
        assertEquals("Save", shortcut.description)
        assertTrue(shortcut.isCustomizable)
    }
    
    @Test
    fun `MouseGesture data class holds values correctly`() {
        val gesture = DesktopModeManager.MouseGesture(
            type = DesktopModeManager.GestureType.RIGHT_CLICK,
            actionId = "context_menu",
            description = "Show context menu"
        )
        
        assertEquals(DesktopModeManager.GestureType.RIGHT_CLICK, gesture.type)
        assertEquals("context_menu", gesture.actionId)
        assertEquals("Show context menu", gesture.description)
    }
    
    @Test
    fun `GestureType enum has correct values`() {
        val types = DesktopModeManager.GestureType.entries
        assertEquals(6, types.size)
        assertTrue(types.contains(DesktopModeManager.GestureType.RIGHT_CLICK))
        assertTrue(types.contains(DesktopModeManager.GestureType.MIDDLE_CLICK))
        assertTrue(types.contains(DesktopModeManager.GestureType.SCROLL_UP))
        assertTrue(types.contains(DesktopModeManager.GestureType.SCROLL_DOWN))
        assertTrue(types.contains(DesktopModeManager.GestureType.DRAG))
        assertTrue(types.contains(DesktopModeManager.GestureType.HOVER))
    }
    
    @Test
    fun `DesktopLayoutConfig data class holds values correctly`() {
        val config = DesktopModeManager.DesktopLayoutConfig(
            showSidebar = true,
            sidebarPosition = DesktopModeManager.SidebarPosition.LEFT,
            showToolbar = true,
            useCompactToolbar = false,
            showStatusBar = true,
            enableMultiWindow = true,
            enableDragAndDrop = true,
            useLargeClickTargets = false
        )
        
        assertTrue(config.showSidebar)
        assertEquals(DesktopModeManager.SidebarPosition.LEFT, config.sidebarPosition)
        assertTrue(config.showToolbar)
        assertFalse(config.useCompactToolbar)
        assertTrue(config.showStatusBar)
        assertTrue(config.enableMultiWindow)
        assertTrue(config.enableDragAndDrop)
        assertFalse(config.useLargeClickTargets)
    }
    
    @Test
    fun `SidebarPosition enum has correct values`() {
        val positions = DesktopModeManager.SidebarPosition.entries
        assertEquals(3, positions.size)
        assertTrue(positions.contains(DesktopModeManager.SidebarPosition.LEFT))
        assertTrue(positions.contains(DesktopModeManager.SidebarPosition.RIGHT))
        assertTrue(positions.contains(DesktopModeManager.SidebarPosition.HIDDEN))
    }
    
    @Test
    fun `WindowConfig data class holds values correctly`() {
        val config = DesktopModeManager.WindowConfig(
            minWidth = 480,
            minHeight = 640,
            defaultWidth = 1024,
            defaultHeight = 768,
            allowResize = true,
            rememberPosition = true
        )
        
        assertEquals(480, config.minWidth)
        assertEquals(640, config.minHeight)
        assertEquals(1024, config.defaultWidth)
        assertEquals(768, config.defaultHeight)
        assertTrue(config.allowResize)
        assertTrue(config.rememberPosition)
    }
    
    @Test
    fun `DesktopLayoutConfig default values are correct`() {
        val defaultConfig = DesktopModeManager.DesktopLayoutConfig()
        
        assertTrue(defaultConfig.showSidebar)
        assertEquals(DesktopModeManager.SidebarPosition.LEFT, defaultConfig.sidebarPosition)
        assertTrue(defaultConfig.showToolbar)
        assertFalse(defaultConfig.useCompactToolbar)
        assertTrue(defaultConfig.showStatusBar)
        assertTrue(defaultConfig.enableMultiWindow)
        assertTrue(defaultConfig.enableDragAndDrop)
        assertFalse(defaultConfig.useLargeClickTargets)
    }
}
