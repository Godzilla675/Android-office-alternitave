package com.officesuite.app.developer

import android.content.Context
import android.view.KeyEvent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Custom Shortcuts Manager for User-Definable Keyboard Shortcuts.
 * Implements Technical Improvements Phase 2 - Section 22: Custom Shortcuts
 * 
 * Features:
 * - User-definable keyboard shortcuts
 * - Default shortcut mappings
 * - Conflict detection
 * - Import/Export shortcuts
 */
class ShortcutManager private constructor(context: Context) {
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    // All registered shortcuts
    private val shortcuts = mutableMapOf<String, Shortcut>()
    
    // Active key bindings (key combo -> action id)
    private val keyBindings = mutableMapOf<KeyCombo, String>()
    
    // Shortcut listeners
    private val listeners = mutableListOf<ShortcutListener>()
    
    init {
        registerDefaultShortcuts()
        loadCustomShortcuts()
    }
    
    /**
     * Register a shortcut.
     */
    fun registerShortcut(shortcut: Shortcut) {
        shortcuts[shortcut.id] = shortcut
        shortcut.keyCombo?.let { keyBindings[it] = shortcut.id }
    }
    
    /**
     * Update a shortcut's key combo.
     */
    fun updateShortcut(shortcutId: String, newKeyCombo: KeyCombo?): ShortcutUpdateResult {
        val shortcut = shortcuts[shortcutId] ?: return ShortcutUpdateResult.NotFound
        
        // Check for conflicts
        if (newKeyCombo != null) {
            val conflictingId = keyBindings[newKeyCombo]
            if (conflictingId != null && conflictingId != shortcutId) {
                val conflicting = shortcuts[conflictingId]
                return ShortcutUpdateResult.Conflict(conflicting?.name ?: conflictingId)
            }
        }
        
        // Remove old binding
        shortcut.keyCombo?.let { keyBindings.remove(it) }
        
        // Update shortcut
        shortcuts[shortcutId] = shortcut.copy(keyCombo = newKeyCombo)
        newKeyCombo?.let { keyBindings[it] = shortcutId }
        
        // Save
        saveCustomShortcuts()
        
        return ShortcutUpdateResult.Success
    }
    
    /**
     * Reset a shortcut to default.
     */
    fun resetShortcut(shortcutId: String) {
        val defaultShortcut = getDefaultShortcuts().find { it.id == shortcutId }
        if (defaultShortcut != null) {
            updateShortcut(shortcutId, defaultShortcut.keyCombo)
        }
    }
    
    /**
     * Reset all shortcuts to defaults.
     */
    fun resetAllShortcuts() {
        keyBindings.clear()
        shortcuts.clear()
        registerDefaultShortcuts()
        prefs.edit().remove(KEY_CUSTOM_SHORTCUTS).apply()
    }
    
    /**
     * Handle a key event.
     * @return true if the key was handled
     */
    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_UP) return false
        
        val keyCombo = KeyCombo(
            keyCode = event.keyCode,
            ctrl = event.isCtrlPressed,
            shift = event.isShiftPressed,
            alt = event.isAltPressed,
            meta = event.isMetaPressed
        )
        
        val shortcutId = keyBindings[keyCombo] ?: return false
        val shortcut = shortcuts[shortcutId] ?: return false
        
        if (!shortcut.isEnabled) return false
        
        // Notify listeners
        listeners.forEach { it.onShortcutTriggered(shortcut) }
        
        return true
    }
    
    /**
     * Get all shortcuts.
     */
    fun getAllShortcuts(): List<Shortcut> = shortcuts.values.toList()
    
    /**
     * Get shortcuts by category.
     */
    fun getShortcutsByCategory(category: ShortcutCategory): List<Shortcut> {
        return shortcuts.values.filter { it.category == category }
    }
    
    /**
     * Get shortcut by ID.
     */
    fun getShortcut(id: String): Shortcut? = shortcuts[id]
    
    /**
     * Add a shortcut listener.
     */
    fun addListener(listener: ShortcutListener) {
        listeners.add(listener)
    }
    
    /**
     * Remove a shortcut listener.
     */
    fun removeListener(listener: ShortcutListener) {
        listeners.remove(listener)
    }
    
    /**
     * Export shortcuts to JSON.
     */
    fun exportShortcuts(): String {
        val exportData = shortcuts.values.map { shortcut ->
            ShortcutExport(
                id = shortcut.id,
                keyCombo = shortcut.keyCombo
            )
        }
        return gson.toJson(exportData)
    }
    
    /**
     * Import shortcuts from JSON.
     */
    fun importShortcuts(json: String): ImportResult {
        return try {
            val type = object : TypeToken<List<ShortcutExport>>() {}.type
            val importData: List<ShortcutExport> = gson.fromJson(json, type)
            
            var imported = 0
            var skipped = 0
            
            importData.forEach { export ->
                if (shortcuts.containsKey(export.id)) {
                    val result = updateShortcut(export.id, export.keyCombo)
                    if (result is ShortcutUpdateResult.Success) imported++ else skipped++
                } else {
                    skipped++
                }
            }
            
            ImportResult.Success(imported, skipped)
        } catch (e: Exception) {
            ImportResult.Error(e.message ?: "Import failed")
        }
    }
    
    /**
     * Get human-readable shortcut text.
     */
    fun getShortcutText(keyCombo: KeyCombo?): String {
        if (keyCombo == null) return "Not set"
        
        return buildString {
            if (keyCombo.ctrl) append("Ctrl+")
            if (keyCombo.shift) append("Shift+")
            if (keyCombo.alt) append("Alt+")
            if (keyCombo.meta) append("Meta+")
            append(keyCodeToString(keyCombo.keyCode))
        }
    }
    
    private fun keyCodeToString(keyCode: Int): String {
        return when (keyCode) {
            KeyEvent.KEYCODE_A -> "A"
            KeyEvent.KEYCODE_B -> "B"
            KeyEvent.KEYCODE_C -> "C"
            KeyEvent.KEYCODE_D -> "D"
            KeyEvent.KEYCODE_E -> "E"
            KeyEvent.KEYCODE_F -> "F"
            KeyEvent.KEYCODE_G -> "G"
            KeyEvent.KEYCODE_H -> "H"
            KeyEvent.KEYCODE_I -> "I"
            KeyEvent.KEYCODE_J -> "J"
            KeyEvent.KEYCODE_K -> "K"
            KeyEvent.KEYCODE_L -> "L"
            KeyEvent.KEYCODE_M -> "M"
            KeyEvent.KEYCODE_N -> "N"
            KeyEvent.KEYCODE_O -> "O"
            KeyEvent.KEYCODE_P -> "P"
            KeyEvent.KEYCODE_Q -> "Q"
            KeyEvent.KEYCODE_R -> "R"
            KeyEvent.KEYCODE_S -> "S"
            KeyEvent.KEYCODE_T -> "T"
            KeyEvent.KEYCODE_U -> "U"
            KeyEvent.KEYCODE_V -> "V"
            KeyEvent.KEYCODE_W -> "W"
            KeyEvent.KEYCODE_X -> "X"
            KeyEvent.KEYCODE_Y -> "Y"
            KeyEvent.KEYCODE_Z -> "Z"
            KeyEvent.KEYCODE_0 -> "0"
            KeyEvent.KEYCODE_1 -> "1"
            KeyEvent.KEYCODE_2 -> "2"
            KeyEvent.KEYCODE_3 -> "3"
            KeyEvent.KEYCODE_4 -> "4"
            KeyEvent.KEYCODE_5 -> "5"
            KeyEvent.KEYCODE_6 -> "6"
            KeyEvent.KEYCODE_7 -> "7"
            KeyEvent.KEYCODE_8 -> "8"
            KeyEvent.KEYCODE_9 -> "9"
            KeyEvent.KEYCODE_F1 -> "F1"
            KeyEvent.KEYCODE_F2 -> "F2"
            KeyEvent.KEYCODE_F3 -> "F3"
            KeyEvent.KEYCODE_F4 -> "F4"
            KeyEvent.KEYCODE_F5 -> "F5"
            KeyEvent.KEYCODE_F6 -> "F6"
            KeyEvent.KEYCODE_F7 -> "F7"
            KeyEvent.KEYCODE_F8 -> "F8"
            KeyEvent.KEYCODE_F9 -> "F9"
            KeyEvent.KEYCODE_F10 -> "F10"
            KeyEvent.KEYCODE_F11 -> "F11"
            KeyEvent.KEYCODE_F12 -> "F12"
            KeyEvent.KEYCODE_ESCAPE -> "Esc"
            KeyEvent.KEYCODE_TAB -> "Tab"
            KeyEvent.KEYCODE_SPACE -> "Space"
            KeyEvent.KEYCODE_ENTER -> "Enter"
            KeyEvent.KEYCODE_DEL -> "Backspace"
            KeyEvent.KEYCODE_FORWARD_DEL -> "Delete"
            KeyEvent.KEYCODE_INSERT -> "Insert"
            KeyEvent.KEYCODE_MOVE_HOME -> "Home"
            KeyEvent.KEYCODE_MOVE_END -> "End"
            KeyEvent.KEYCODE_PAGE_UP -> "PageUp"
            KeyEvent.KEYCODE_PAGE_DOWN -> "PageDown"
            KeyEvent.KEYCODE_DPAD_UP -> "↑"
            KeyEvent.KEYCODE_DPAD_DOWN -> "↓"
            KeyEvent.KEYCODE_DPAD_LEFT -> "←"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "→"
            KeyEvent.KEYCODE_PLUS -> "+"
            KeyEvent.KEYCODE_MINUS -> "-"
            KeyEvent.KEYCODE_EQUALS -> "="
            KeyEvent.KEYCODE_COMMA -> ","
            KeyEvent.KEYCODE_PERIOD -> "."
            KeyEvent.KEYCODE_SLASH -> "/"
            KeyEvent.KEYCODE_BACKSLASH -> "\\"
            else -> "Key$keyCode"
        }
    }
    
    private fun registerDefaultShortcuts() {
        getDefaultShortcuts().forEach { registerShortcut(it) }
    }
    
    private fun getDefaultShortcuts(): List<Shortcut> = listOf(
        // File operations
        Shortcut(
            id = "file_new",
            name = "New Document",
            description = "Create a new document",
            category = ShortcutCategory.FILE,
            keyCombo = KeyCombo(KeyEvent.KEYCODE_N, ctrl = true),
            action = ShortcutAction.FILE_NEW
        ),
        Shortcut(
            id = "file_open",
            name = "Open File",
            description = "Open an existing file",
            category = ShortcutCategory.FILE,
            keyCombo = KeyCombo(KeyEvent.KEYCODE_O, ctrl = true),
            action = ShortcutAction.FILE_OPEN
        ),
        Shortcut(
            id = "file_save",
            name = "Save",
            description = "Save current document",
            category = ShortcutCategory.FILE,
            keyCombo = KeyCombo(KeyEvent.KEYCODE_S, ctrl = true),
            action = ShortcutAction.FILE_SAVE
        ),
        Shortcut(
            id = "file_save_as",
            name = "Save As",
            description = "Save document with new name",
            category = ShortcutCategory.FILE,
            keyCombo = KeyCombo(KeyEvent.KEYCODE_S, ctrl = true, shift = true),
            action = ShortcutAction.FILE_SAVE_AS
        ),
        Shortcut(
            id = "file_export_pdf",
            name = "Export to PDF",
            description = "Export document as PDF",
            category = ShortcutCategory.FILE,
            keyCombo = KeyCombo(KeyEvent.KEYCODE_E, ctrl = true, shift = true),
            action = ShortcutAction.FILE_EXPORT_PDF
        ),
        Shortcut(
            id = "file_print",
            name = "Print",
            description = "Print current document",
            category = ShortcutCategory.FILE,
            keyCombo = KeyCombo(KeyEvent.KEYCODE_P, ctrl = true),
            action = ShortcutAction.FILE_PRINT
        ),
        
        // Edit operations
        Shortcut(
            id = "edit_undo",
            name = "Undo",
            description = "Undo last action",
            category = ShortcutCategory.EDIT,
            keyCombo = KeyCombo(KeyEvent.KEYCODE_Z, ctrl = true),
            action = ShortcutAction.EDIT_UNDO
        ),
        Shortcut(
            id = "edit_redo",
            name = "Redo",
            description = "Redo last action",
            category = ShortcutCategory.EDIT,
            keyCombo = KeyCombo(KeyEvent.KEYCODE_Z, ctrl = true, shift = true),
            action = ShortcutAction.EDIT_REDO
        ),
        Shortcut(
            id = "edit_cut",
            name = "Cut",
            description = "Cut selected content",
            category = ShortcutCategory.EDIT,
            keyCombo = KeyCombo(KeyEvent.KEYCODE_X, ctrl = true),
            action = ShortcutAction.EDIT_CUT
        ),
        Shortcut(
            id = "edit_copy",
            name = "Copy",
            description = "Copy selected content",
            category = ShortcutCategory.EDIT,
            keyCombo = KeyCombo(KeyEvent.KEYCODE_C, ctrl = true),
            action = ShortcutAction.EDIT_COPY
        ),
        Shortcut(
            id = "edit_paste",
            name = "Paste",
            description = "Paste from clipboard",
            category = ShortcutCategory.EDIT,
            keyCombo = KeyCombo(KeyEvent.KEYCODE_V, ctrl = true),
            action = ShortcutAction.EDIT_PASTE
        ),
        Shortcut(
            id = "edit_select_all",
            name = "Select All",
            description = "Select all content",
            category = ShortcutCategory.EDIT,
            keyCombo = KeyCombo(KeyEvent.KEYCODE_A, ctrl = true),
            action = ShortcutAction.EDIT_SELECT_ALL
        ),
        Shortcut(
            id = "edit_find",
            name = "Find",
            description = "Find text in document",
            category = ShortcutCategory.EDIT,
            keyCombo = KeyCombo(KeyEvent.KEYCODE_F, ctrl = true),
            action = ShortcutAction.EDIT_FIND
        ),
        Shortcut(
            id = "edit_replace",
            name = "Find and Replace",
            description = "Find and replace text",
            category = ShortcutCategory.EDIT,
            keyCombo = KeyCombo(KeyEvent.KEYCODE_H, ctrl = true),
            action = ShortcutAction.EDIT_REPLACE
        ),
        
        // View operations
        Shortcut(
            id = "view_zoom_in",
            name = "Zoom In",
            description = "Increase zoom level",
            category = ShortcutCategory.VIEW,
            keyCombo = KeyCombo(KeyEvent.KEYCODE_PLUS, ctrl = true),
            action = ShortcutAction.VIEW_ZOOM_IN
        ),
        Shortcut(
            id = "view_zoom_out",
            name = "Zoom Out",
            description = "Decrease zoom level",
            category = ShortcutCategory.VIEW,
            keyCombo = KeyCombo(KeyEvent.KEYCODE_MINUS, ctrl = true),
            action = ShortcutAction.VIEW_ZOOM_OUT
        ),
        Shortcut(
            id = "view_zoom_reset",
            name = "Reset Zoom",
            description = "Reset to 100% zoom",
            category = ShortcutCategory.VIEW,
            keyCombo = KeyCombo(KeyEvent.KEYCODE_0, ctrl = true),
            action = ShortcutAction.VIEW_ZOOM_RESET
        ),
        Shortcut(
            id = "view_focus_mode",
            name = "Focus Mode",
            description = "Toggle focus mode",
            category = ShortcutCategory.VIEW,
            keyCombo = KeyCombo(KeyEvent.KEYCODE_F11),
            action = ShortcutAction.VIEW_FOCUS_MODE
        ),
        Shortcut(
            id = "view_fullscreen",
            name = "Fullscreen",
            description = "Toggle fullscreen mode",
            category = ShortcutCategory.VIEW,
            keyCombo = KeyCombo(KeyEvent.KEYCODE_F, ctrl = true, shift = true),
            action = ShortcutAction.VIEW_FULLSCREEN
        ),
        
        // Navigation
        Shortcut(
            id = "nav_next_page",
            name = "Next Page",
            description = "Go to next page",
            category = ShortcutCategory.NAVIGATION,
            keyCombo = KeyCombo(KeyEvent.KEYCODE_PAGE_DOWN),
            action = ShortcutAction.NAV_NEXT_PAGE
        ),
        Shortcut(
            id = "nav_prev_page",
            name = "Previous Page",
            description = "Go to previous page",
            category = ShortcutCategory.NAVIGATION,
            keyCombo = KeyCombo(KeyEvent.KEYCODE_PAGE_UP),
            action = ShortcutAction.NAV_PREV_PAGE
        ),
        Shortcut(
            id = "nav_go_to_page",
            name = "Go to Page",
            description = "Jump to specific page",
            category = ShortcutCategory.NAVIGATION,
            keyCombo = KeyCombo(KeyEvent.KEYCODE_G, ctrl = true),
            action = ShortcutAction.NAV_GO_TO_PAGE
        ),
        
        // Tools
        Shortcut(
            id = "tools_command_palette",
            name = "Command Palette",
            description = "Open command palette",
            category = ShortcutCategory.TOOLS,
            keyCombo = KeyCombo(KeyEvent.KEYCODE_P, ctrl = true, shift = true),
            action = ShortcutAction.TOOLS_COMMAND_PALETTE
        ),
        Shortcut(
            id = "tools_settings",
            name = "Settings",
            description = "Open settings",
            category = ShortcutCategory.TOOLS,
            keyCombo = KeyCombo(KeyEvent.KEYCODE_COMMA, ctrl = true),
            action = ShortcutAction.TOOLS_SETTINGS
        ),
        Shortcut(
            id = "tools_scanner",
            name = "Scanner",
            description = "Open document scanner",
            category = ShortcutCategory.TOOLS,
            keyCombo = KeyCombo(KeyEvent.KEYCODE_D, ctrl = true, shift = true),
            action = ShortcutAction.TOOLS_SCANNER
        )
    )
    
    private fun loadCustomShortcuts() {
        val json = prefs.getString(KEY_CUSTOM_SHORTCUTS, null) ?: return
        try {
            val type = object : TypeToken<List<ShortcutExport>>() {}.type
            val customData: List<ShortcutExport> = gson.fromJson(json, type)
            
            customData.forEach { export ->
                shortcuts[export.id]?.let { shortcut ->
                    shortcuts[export.id] = shortcut.copy(keyCombo = export.keyCombo)
                    shortcut.keyCombo?.let { keyBindings.remove(it) }
                    export.keyCombo?.let { keyBindings[it] = export.id }
                }
            }
        } catch (e: Exception) {
            // Ignore and use defaults
        }
    }
    
    private fun saveCustomShortcuts() {
        val customData = shortcuts.values.map { shortcut ->
            ShortcutExport(
                id = shortcut.id,
                keyCombo = shortcut.keyCombo
            )
        }
        val json = gson.toJson(customData)
        prefs.edit().putString(KEY_CUSTOM_SHORTCUTS, json).apply()
    }
    
    companion object {
        private const val PREFS_NAME = "shortcuts_prefs"
        private const val KEY_CUSTOM_SHORTCUTS = "custom_shortcuts"
        
        @Volatile
        private var instance: ShortcutManager? = null
        
        fun getInstance(context: Context): ShortcutManager {
            return instance ?: synchronized(this) {
                instance ?: ShortcutManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

/**
 * Keyboard shortcut representation.
 */
data class Shortcut(
    val id: String,
    val name: String,
    val description: String,
    val category: ShortcutCategory,
    val keyCombo: KeyCombo?,
    val action: ShortcutAction,
    val isEnabled: Boolean = true
)

/**
 * Key combination.
 */
data class KeyCombo(
    val keyCode: Int,
    val ctrl: Boolean = false,
    val shift: Boolean = false,
    val alt: Boolean = false,
    val meta: Boolean = false
)

/**
 * Shortcut categories.
 */
enum class ShortcutCategory(val displayName: String) {
    FILE("File"),
    EDIT("Edit"),
    VIEW("View"),
    NAVIGATION("Navigation"),
    TOOLS("Tools"),
    CUSTOM("Custom")
}

/**
 * Shortcut actions.
 */
enum class ShortcutAction {
    // File
    FILE_NEW,
    FILE_OPEN,
    FILE_SAVE,
    FILE_SAVE_AS,
    FILE_EXPORT_PDF,
    FILE_PRINT,
    FILE_CLOSE,
    
    // Edit
    EDIT_UNDO,
    EDIT_REDO,
    EDIT_CUT,
    EDIT_COPY,
    EDIT_PASTE,
    EDIT_SELECT_ALL,
    EDIT_FIND,
    EDIT_REPLACE,
    
    // View
    VIEW_ZOOM_IN,
    VIEW_ZOOM_OUT,
    VIEW_ZOOM_RESET,
    VIEW_FOCUS_MODE,
    VIEW_FULLSCREEN,
    
    // Navigation
    NAV_NEXT_PAGE,
    NAV_PREV_PAGE,
    NAV_GO_TO_PAGE,
    NAV_FIRST_PAGE,
    NAV_LAST_PAGE,
    
    // Tools
    TOOLS_COMMAND_PALETTE,
    TOOLS_SETTINGS,
    TOOLS_SCANNER,
    TOOLS_CONVERTER,
    
    // Custom
    CUSTOM
}

/**
 * Shortcut listener interface.
 */
interface ShortcutListener {
    fun onShortcutTriggered(shortcut: Shortcut)
}

/**
 * Result of updating a shortcut.
 */
sealed class ShortcutUpdateResult {
    object Success : ShortcutUpdateResult()
    object NotFound : ShortcutUpdateResult()
    data class Conflict(val conflictingName: String) : ShortcutUpdateResult()
}

/**
 * Result of importing shortcuts.
 */
sealed class ImportResult {
    data class Success(val imported: Int, val skipped: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

/**
 * Shortcut export data.
 */
private data class ShortcutExport(
    val id: String,
    val keyCombo: KeyCombo?
)
