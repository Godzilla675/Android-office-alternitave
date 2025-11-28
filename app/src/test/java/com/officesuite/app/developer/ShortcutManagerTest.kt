package com.officesuite.app.developer

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the ShortcutManager class.
 */
class ShortcutManagerTest {

    @Test
    fun `KeyCombo equals works correctly for identical combos`() {
        val combo1 = KeyCombo(
            keyCode = 83, // S
            ctrl = true,
            shift = false,
            alt = false,
            meta = false
        )
        val combo2 = KeyCombo(
            keyCode = 83,
            ctrl = true,
            shift = false,
            alt = false,
            meta = false
        )
        
        assertEquals(combo1, combo2)
    }

    @Test
    fun `KeyCombo equals works correctly for different combos`() {
        val combo1 = KeyCombo(keyCode = 83, ctrl = true)
        val combo2 = KeyCombo(keyCode = 83, ctrl = true, shift = true)
        
        assertNotEquals(combo1, combo2)
    }

    @Test
    fun `KeyCombo hashCode is consistent`() {
        val combo1 = KeyCombo(keyCode = 83, ctrl = true)
        val combo2 = KeyCombo(keyCode = 83, ctrl = true)
        
        assertEquals(combo1.hashCode(), combo2.hashCode())
    }

    @Test
    fun `Shortcut data class holds values correctly`() {
        val shortcut = Shortcut(
            id = "test_shortcut",
            name = "Test Shortcut",
            description = "A test shortcut",
            category = ShortcutCategory.FILE,
            keyCombo = KeyCombo(keyCode = 83, ctrl = true),
            action = ShortcutAction.FILE_SAVE,
            isEnabled = true
        )
        
        assertEquals("test_shortcut", shortcut.id)
        assertEquals("Test Shortcut", shortcut.name)
        assertEquals("A test shortcut", shortcut.description)
        assertEquals(ShortcutCategory.FILE, shortcut.category)
        assertEquals(ShortcutAction.FILE_SAVE, shortcut.action)
        assertTrue(shortcut.isEnabled)
    }

    @Test
    fun `ShortcutCategory enum has expected values`() {
        val categories = ShortcutCategory.values()
        
        assertTrue(categories.contains(ShortcutCategory.FILE))
        assertTrue(categories.contains(ShortcutCategory.EDIT))
        assertTrue(categories.contains(ShortcutCategory.VIEW))
        assertTrue(categories.contains(ShortcutCategory.NAVIGATION))
        assertTrue(categories.contains(ShortcutCategory.TOOLS))
        assertTrue(categories.contains(ShortcutCategory.CUSTOM))
    }

    @Test
    fun `ShortcutCategory has correct display names`() {
        assertEquals("File", ShortcutCategory.FILE.displayName)
        assertEquals("Edit", ShortcutCategory.EDIT.displayName)
        assertEquals("View", ShortcutCategory.VIEW.displayName)
        assertEquals("Navigation", ShortcutCategory.NAVIGATION.displayName)
        assertEquals("Tools", ShortcutCategory.TOOLS.displayName)
        assertEquals("Custom", ShortcutCategory.CUSTOM.displayName)
    }

    @Test
    fun `ShortcutAction enum has file actions`() {
        val actions = ShortcutAction.values()
        
        assertTrue(actions.contains(ShortcutAction.FILE_NEW))
        assertTrue(actions.contains(ShortcutAction.FILE_OPEN))
        assertTrue(actions.contains(ShortcutAction.FILE_SAVE))
        assertTrue(actions.contains(ShortcutAction.FILE_SAVE_AS))
        assertTrue(actions.contains(ShortcutAction.FILE_EXPORT_PDF))
        assertTrue(actions.contains(ShortcutAction.FILE_PRINT))
    }

    @Test
    fun `ShortcutAction enum has edit actions`() {
        val actions = ShortcutAction.values()
        
        assertTrue(actions.contains(ShortcutAction.EDIT_UNDO))
        assertTrue(actions.contains(ShortcutAction.EDIT_REDO))
        assertTrue(actions.contains(ShortcutAction.EDIT_CUT))
        assertTrue(actions.contains(ShortcutAction.EDIT_COPY))
        assertTrue(actions.contains(ShortcutAction.EDIT_PASTE))
        assertTrue(actions.contains(ShortcutAction.EDIT_FIND))
    }

    @Test
    fun `ShortcutAction enum has view actions`() {
        val actions = ShortcutAction.values()
        
        assertTrue(actions.contains(ShortcutAction.VIEW_ZOOM_IN))
        assertTrue(actions.contains(ShortcutAction.VIEW_ZOOM_OUT))
        assertTrue(actions.contains(ShortcutAction.VIEW_FOCUS_MODE))
        assertTrue(actions.contains(ShortcutAction.VIEW_FULLSCREEN))
    }

    @Test
    fun `ShortcutUpdateResult Success singleton is consistent`() {
        val result1 = ShortcutUpdateResult.Success
        val result2 = ShortcutUpdateResult.Success
        
        assertSame(result1, result2)
    }

    @Test
    fun `ShortcutUpdateResult Conflict contains name`() {
        val result = ShortcutUpdateResult.Conflict("Save")
        
        assertTrue(result is ShortcutUpdateResult.Conflict)
        assertEquals("Save", result.conflictingName)
    }

    @Test
    fun `ImportResult Success contains counts`() {
        val result = ImportResult.Success(imported = 5, skipped = 2)
        
        assertTrue(result is ImportResult.Success)
        assertEquals(5, result.imported)
        assertEquals(2, result.skipped)
    }

    @Test
    fun `ImportResult Error contains message`() {
        val result = ImportResult.Error("Invalid format")
        
        assertTrue(result is ImportResult.Error)
        assertEquals("Invalid format", result.message)
    }
}
