package com.officesuite.app.platform

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.abs

/**
 * Manages Samsung DeX mode and ChromeOS keyboard/mouse optimization.
 * Implements Phase 2 Platform-Specific Features:
 * - Section 25: DeX Mode Support - Desktop experience on Samsung DeX
 * - Section 25: ChromeOS Optimization - Full ChromeOS keyboard/mouse support
 *
 * Provides enhanced desktop-like experience when running on Samsung DeX
 * or ChromeOS devices with full keyboard and mouse support.
 */
class DesktopModeManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    /**
     * Desktop mode state
     */
    data class DesktopModeState(
        val isDesktopMode: Boolean,
        val isDexMode: Boolean,
        val isChromeOS: Boolean,
        val hasPhysicalKeyboard: Boolean,
        val hasMouseConnected: Boolean,
        val screenSize: ScreenSize
    )
    
    /**
     * Screen size categories
     */
    enum class ScreenSize {
        COMPACT,    // Phone
        MEDIUM,     // Small tablet
        EXPANDED    // Large tablet, desktop
    }
    
    /**
     * Keyboard shortcut definition
     */
    data class KeyboardShortcut(
        val keyCode: Int,
        val modifiers: Int, // Combination of META, CTRL, ALT, SHIFT
        val actionId: String,
        val description: String,
        val isCustomizable: Boolean = true
    )
    
    /**
     * Mouse gesture definition
     */
    data class MouseGesture(
        val type: GestureType,
        val actionId: String,
        val description: String
    )
    
    /**
     * Gesture types for mouse
     */
    enum class GestureType {
        RIGHT_CLICK,
        MIDDLE_CLICK,
        SCROLL_UP,
        SCROLL_DOWN,
        DRAG,
        HOVER
    }
    
    /**
     * Desktop layout options
     */
    data class DesktopLayoutConfig(
        val showSidebar: Boolean = true,
        val sidebarPosition: SidebarPosition = SidebarPosition.LEFT,
        val showToolbar: Boolean = true,
        val useCompactToolbar: Boolean = false,
        val showStatusBar: Boolean = true,
        val enableMultiWindow: Boolean = true,
        val enableDragAndDrop: Boolean = true,
        val useLargeClickTargets: Boolean = false
    )
    
    /**
     * Sidebar positions
     */
    enum class SidebarPosition {
        LEFT,
        RIGHT,
        HIDDEN
    }
    
    /**
     * Window resize configuration
     */
    data class WindowConfig(
        val minWidth: Int,
        val minHeight: Int,
        val defaultWidth: Int,
        val defaultHeight: Int,
        val allowResize: Boolean,
        val rememberPosition: Boolean
    )
    
    /**
     * Check if device is in DeX mode
     */
    fun isInDexMode(): Boolean {
        return try {
            val config = context.resources.configuration
            val dexMode = Configuration::class.java.getField("SEM_DESKTOP_MODE_ENABLED")
            val mode = config.javaClass.getField("semDesktopModeEnabled").getInt(config)
            mode == dexMode.getInt(null)
        } catch (e: Exception) {
            // DeX mode detection not available
            false
        }
    }
    
    /**
     * Check if running on ChromeOS
     */
    fun isChromeOS(): Boolean {
        return context.packageManager.hasSystemFeature("org.chromium.arc") ||
               context.packageManager.hasSystemFeature("org.chromium.arc.device_management")
    }
    
    /**
     * Check if a physical keyboard is connected
     */
    fun hasPhysicalKeyboard(): Boolean {
        val config = context.resources.configuration
        return config.keyboard == Configuration.KEYBOARD_QWERTY
    }
    
    /**
     * Check if a mouse/trackpad is connected
     */
    fun hasMouseConnected(): Boolean {
        val config = context.resources.configuration
        return config.navigation == Configuration.NAVIGATION_TRACKBALL ||
               config.touchscreen == Configuration.TOUCHSCREEN_NOTOUCH
    }
    
    /**
     * Check if in any desktop mode
     */
    fun isDesktopMode(): Boolean {
        return isInDexMode() || isChromeOS() || hasPhysicalKeyboard()
    }
    
    /**
     * Get current desktop mode state
     */
    fun getDesktopModeState(): DesktopModeState {
        return DesktopModeState(
            isDesktopMode = isDesktopMode(),
            isDexMode = isInDexMode(),
            isChromeOS = isChromeOS(),
            hasPhysicalKeyboard = hasPhysicalKeyboard(),
            hasMouseConnected = hasMouseConnected(),
            screenSize = getScreenSize()
        )
    }
    
    /**
     * Get screen size category
     */
    fun getScreenSize(): ScreenSize {
        val config = context.resources.configuration
        val screenLayout = config.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        
        return when {
            screenLayout >= Configuration.SCREENLAYOUT_SIZE_XLARGE -> ScreenSize.EXPANDED
            screenLayout >= Configuration.SCREENLAYOUT_SIZE_LARGE -> ScreenSize.EXPANDED
            screenLayout >= Configuration.SCREENLAYOUT_SIZE_NORMAL -> ScreenSize.MEDIUM
            else -> ScreenSize.COMPACT
        }
    }
    
    /**
     * Get default keyboard shortcuts
     */
    fun getDefaultShortcuts(): List<KeyboardShortcut> {
        return listOf(
            // File operations
            KeyboardShortcut(
                keyCode = KeyEvent.KEYCODE_O,
                modifiers = KeyEvent.META_CTRL_ON,
                actionId = "open_file",
                description = "Open file"
            ),
            KeyboardShortcut(
                keyCode = KeyEvent.KEYCODE_S,
                modifiers = KeyEvent.META_CTRL_ON,
                actionId = "save",
                description = "Save"
            ),
            KeyboardShortcut(
                keyCode = KeyEvent.KEYCODE_S,
                modifiers = KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_ON,
                actionId = "save_as",
                description = "Save as"
            ),
            KeyboardShortcut(
                keyCode = KeyEvent.KEYCODE_N,
                modifiers = KeyEvent.META_CTRL_ON,
                actionId = "new_document",
                description = "New document"
            ),
            KeyboardShortcut(
                keyCode = KeyEvent.KEYCODE_P,
                modifiers = KeyEvent.META_CTRL_ON,
                actionId = "print",
                description = "Print"
            ),
            KeyboardShortcut(
                keyCode = KeyEvent.KEYCODE_W,
                modifiers = KeyEvent.META_CTRL_ON,
                actionId = "close",
                description = "Close"
            ),
            
            // Edit operations
            KeyboardShortcut(
                keyCode = KeyEvent.KEYCODE_Z,
                modifiers = KeyEvent.META_CTRL_ON,
                actionId = "undo",
                description = "Undo"
            ),
            KeyboardShortcut(
                keyCode = KeyEvent.KEYCODE_Y,
                modifiers = KeyEvent.META_CTRL_ON,
                actionId = "redo",
                description = "Redo"
            ),
            KeyboardShortcut(
                keyCode = KeyEvent.KEYCODE_Z,
                modifiers = KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_ON,
                actionId = "redo",
                description = "Redo"
            ),
            KeyboardShortcut(
                keyCode = KeyEvent.KEYCODE_C,
                modifiers = KeyEvent.META_CTRL_ON,
                actionId = "copy",
                description = "Copy"
            ),
            KeyboardShortcut(
                keyCode = KeyEvent.KEYCODE_X,
                modifiers = KeyEvent.META_CTRL_ON,
                actionId = "cut",
                description = "Cut"
            ),
            KeyboardShortcut(
                keyCode = KeyEvent.KEYCODE_V,
                modifiers = KeyEvent.META_CTRL_ON,
                actionId = "paste",
                description = "Paste"
            ),
            KeyboardShortcut(
                keyCode = KeyEvent.KEYCODE_A,
                modifiers = KeyEvent.META_CTRL_ON,
                actionId = "select_all",
                description = "Select all"
            ),
            KeyboardShortcut(
                keyCode = KeyEvent.KEYCODE_F,
                modifiers = KeyEvent.META_CTRL_ON,
                actionId = "find",
                description = "Find"
            ),
            KeyboardShortcut(
                keyCode = KeyEvent.KEYCODE_H,
                modifiers = KeyEvent.META_CTRL_ON,
                actionId = "find_replace",
                description = "Find and replace"
            ),
            
            // View operations
            KeyboardShortcut(
                keyCode = KeyEvent.KEYCODE_PLUS,
                modifiers = KeyEvent.META_CTRL_ON,
                actionId = "zoom_in",
                description = "Zoom in"
            ),
            KeyboardShortcut(
                keyCode = KeyEvent.KEYCODE_MINUS,
                modifiers = KeyEvent.META_CTRL_ON,
                actionId = "zoom_out",
                description = "Zoom out"
            ),
            KeyboardShortcut(
                keyCode = KeyEvent.KEYCODE_0,
                modifiers = KeyEvent.META_CTRL_ON,
                actionId = "zoom_reset",
                description = "Reset zoom"
            ),
            KeyboardShortcut(
                keyCode = KeyEvent.KEYCODE_F11,
                modifiers = 0,
                actionId = "fullscreen",
                description = "Toggle fullscreen"
            ),
            
            // Navigation
            KeyboardShortcut(
                keyCode = KeyEvent.KEYCODE_PAGE_DOWN,
                modifiers = 0,
                actionId = "next_page",
                description = "Next page"
            ),
            KeyboardShortcut(
                keyCode = KeyEvent.KEYCODE_PAGE_UP,
                modifiers = 0,
                actionId = "previous_page",
                description = "Previous page"
            ),
            KeyboardShortcut(
                keyCode = KeyEvent.KEYCODE_HOME,
                modifiers = KeyEvent.META_CTRL_ON,
                actionId = "go_to_start",
                description = "Go to start"
            ),
            KeyboardShortcut(
                keyCode = KeyEvent.KEYCODE_MOVE_END,
                modifiers = KeyEvent.META_CTRL_ON,
                actionId = "go_to_end",
                description = "Go to end"
            ),
            KeyboardShortcut(
                keyCode = KeyEvent.KEYCODE_G,
                modifiers = KeyEvent.META_CTRL_ON,
                actionId = "go_to_page",
                description = "Go to page"
            ),
            
            // Formatting (for editors)
            KeyboardShortcut(
                keyCode = KeyEvent.KEYCODE_B,
                modifiers = KeyEvent.META_CTRL_ON,
                actionId = "bold",
                description = "Bold"
            ),
            KeyboardShortcut(
                keyCode = KeyEvent.KEYCODE_I,
                modifiers = KeyEvent.META_CTRL_ON,
                actionId = "italic",
                description = "Italic"
            ),
            KeyboardShortcut(
                keyCode = KeyEvent.KEYCODE_U,
                modifiers = KeyEvent.META_CTRL_ON,
                actionId = "underline",
                description = "Underline"
            ),
            
            // App-specific
            KeyboardShortcut(
                keyCode = KeyEvent.KEYCODE_D,
                modifiers = KeyEvent.META_CTRL_ON,
                actionId = "scan_document",
                description = "Scan document"
            ),
            KeyboardShortcut(
                keyCode = KeyEvent.KEYCODE_E,
                modifiers = KeyEvent.META_CTRL_ON,
                actionId = "convert",
                description = "Convert document"
            ),
            KeyboardShortcut(
                keyCode = KeyEvent.KEYCODE_COMMA,
                modifiers = KeyEvent.META_CTRL_ON,
                actionId = "settings",
                description = "Open settings"
            ),
            KeyboardShortcut(
                keyCode = KeyEvent.KEYCODE_SLASH,
                modifiers = KeyEvent.META_CTRL_ON,
                actionId = "show_shortcuts",
                description = "Show keyboard shortcuts"
            )
        )
    }
    
    /**
     * Get custom shortcuts
     */
    fun getCustomShortcuts(): List<KeyboardShortcut> {
        // Load from preferences
        return getDefaultShortcuts() // For now, return defaults
    }
    
    /**
     * Handle keyboard shortcut
     */
    fun handleKeyEvent(event: KeyEvent, listener: ShortcutListener): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        
        val shortcuts = getCustomShortcuts()
        val matchingShortcut = shortcuts.find { shortcut ->
            shortcut.keyCode == event.keyCode &&
            checkModifiers(event, shortcut.modifiers)
        }
        
        return matchingShortcut?.let {
            listener.onShortcutTriggered(it.actionId)
            true
        } ?: false
    }
    
    private fun checkModifiers(event: KeyEvent, requiredModifiers: Int): Boolean {
        val ctrlRequired = (requiredModifiers and KeyEvent.META_CTRL_ON) != 0
        val shiftRequired = (requiredModifiers and KeyEvent.META_SHIFT_ON) != 0
        val altRequired = (requiredModifiers and KeyEvent.META_ALT_ON) != 0
        val metaRequired = (requiredModifiers and KeyEvent.META_META_ON) != 0
        
        return event.isCtrlPressed == ctrlRequired &&
               event.isShiftPressed == shiftRequired &&
               event.isAltPressed == altRequired &&
               event.isMetaPressed == metaRequired
    }
    
    /**
     * Get desktop layout configuration
     */
    fun getDesktopLayoutConfig(): DesktopLayoutConfig {
        return DesktopLayoutConfig(
            showSidebar = prefs.getBoolean(KEY_SHOW_SIDEBAR, true),
            sidebarPosition = SidebarPosition.entries.find {
                it.name == prefs.getString(KEY_SIDEBAR_POSITION, SidebarPosition.LEFT.name)
            } ?: SidebarPosition.LEFT,
            showToolbar = prefs.getBoolean(KEY_SHOW_TOOLBAR, true),
            useCompactToolbar = prefs.getBoolean(KEY_COMPACT_TOOLBAR, false),
            showStatusBar = prefs.getBoolean(KEY_SHOW_STATUS_BAR, true),
            enableMultiWindow = prefs.getBoolean(KEY_MULTI_WINDOW, true),
            enableDragAndDrop = prefs.getBoolean(KEY_DRAG_DROP, true),
            useLargeClickTargets = prefs.getBoolean(KEY_LARGE_TARGETS, false)
        )
    }
    
    /**
     * Save desktop layout configuration
     */
    fun saveDesktopLayoutConfig(config: DesktopLayoutConfig) {
        prefs.edit().apply {
            putBoolean(KEY_SHOW_SIDEBAR, config.showSidebar)
            putString(KEY_SIDEBAR_POSITION, config.sidebarPosition.name)
            putBoolean(KEY_SHOW_TOOLBAR, config.showToolbar)
            putBoolean(KEY_COMPACT_TOOLBAR, config.useCompactToolbar)
            putBoolean(KEY_SHOW_STATUS_BAR, config.showStatusBar)
            putBoolean(KEY_MULTI_WINDOW, config.enableMultiWindow)
            putBoolean(KEY_DRAG_DROP, config.enableDragAndDrop)
            putBoolean(KEY_LARGE_TARGETS, config.useLargeClickTargets)
            apply()
        }
    }
    
    /**
     * Get window configuration
     */
    fun getWindowConfig(): WindowConfig {
        return WindowConfig(
            minWidth = prefs.getInt(KEY_MIN_WIDTH, 480),
            minHeight = prefs.getInt(KEY_MIN_HEIGHT, 640),
            defaultWidth = prefs.getInt(KEY_DEFAULT_WIDTH, 1024),
            defaultHeight = prefs.getInt(KEY_DEFAULT_HEIGHT, 768),
            allowResize = prefs.getBoolean(KEY_ALLOW_RESIZE, true),
            rememberPosition = prefs.getBoolean(KEY_REMEMBER_POSITION, true)
        )
    }
    
    /**
     * Enable or disable hover effects
     */
    fun enableHoverEffects(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            view.setOnGenericMotionListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_HOVER_ENTER -> {
                        v.alpha = 0.9f
                        true
                    }
                    MotionEvent.ACTION_HOVER_EXIT -> {
                        v.alpha = 1.0f
                        true
                    }
                    else -> false
                }
            }
        }
    }
    
    /**
     * Enable right-click context menu
     */
    fun enableRightClickContextMenu(view: View, listener: ContextMenuListener) {
        view.setOnGenericMotionListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_BUTTON_PRESS &&
                event.actionButton == MotionEvent.BUTTON_SECONDARY) {
                listener.onShowContextMenu(event.x, event.y)
                true
            } else {
                false
            }
        }
    }
    
    /**
     * Handle mouse scroll for zoom
     */
    fun handleMouseScroll(event: MotionEvent, listener: ScrollListener): Boolean {
        if (event.actionMasked != MotionEvent.ACTION_SCROLL) return false
        
        val scrollY = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
        val isCtrlPressed = (event.metaState and KeyEvent.META_CTRL_ON) != 0
        return when {
            isCtrlPressed -> {
                // Zoom with Ctrl+Scroll
                if (scrollY > 0) {
                    listener.onZoomIn()
                } else {
                    listener.onZoomOut()
                }
                true
            }
            abs(scrollY) > 0 -> {
                // Normal scroll
                listener.onScroll(scrollY)
                true
            }
            else -> false
        }
    }
    
    /**
     * Interface for keyboard shortcut handling
     */
    interface ShortcutListener {
        fun onShortcutTriggered(actionId: String)
    }
    
    /**
     * Interface for context menu
     */
    interface ContextMenuListener {
        fun onShowContextMenu(x: Float, y: Float)
    }
    
    /**
     * Interface for scroll/zoom handling
     */
    interface ScrollListener {
        fun onScroll(amount: Float)
        fun onZoomIn()
        fun onZoomOut()
    }
    
    companion object {
        private const val PREFS_NAME = "desktop_mode_prefs"
        private const val KEY_SHOW_SIDEBAR = "show_sidebar"
        private const val KEY_SIDEBAR_POSITION = "sidebar_position"
        private const val KEY_SHOW_TOOLBAR = "show_toolbar"
        private const val KEY_COMPACT_TOOLBAR = "compact_toolbar"
        private const val KEY_SHOW_STATUS_BAR = "show_status_bar"
        private const val KEY_MULTI_WINDOW = "multi_window"
        private const val KEY_DRAG_DROP = "drag_drop"
        private const val KEY_LARGE_TARGETS = "large_targets"
        private const val KEY_MIN_WIDTH = "min_width"
        private const val KEY_MIN_HEIGHT = "min_height"
        private const val KEY_DEFAULT_WIDTH = "default_width"
        private const val KEY_DEFAULT_HEIGHT = "default_height"
        private const val KEY_ALLOW_RESIZE = "allow_resize"
        private const val KEY_REMEMBER_POSITION = "remember_position"
    }
}
