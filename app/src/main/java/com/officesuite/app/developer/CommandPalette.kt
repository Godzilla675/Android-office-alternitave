package com.officesuite.app.developer

import android.content.Context
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.officesuite.app.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Command Palette for Quick Access to All Commands.
 * Implements Technical Improvements Phase 2 - Section 22: Command Palette
 * 
 * Features:
 * - Quick access to all app commands (Ctrl+Shift+P style)
 * - Fuzzy search filtering
 * - Keyboard navigation
 * - Recently used commands
 * - Command categories
 */
class CommandPalette(private val context: Context) {
    
    private val commands = mutableListOf<Command>()
    private val recentCommands = mutableListOf<String>()
    
    private val _filteredCommands = MutableStateFlow<List<Command>>(emptyList())
    val filteredCommands: StateFlow<List<Command>> = _filteredCommands
    
    private var onCommandExecuted: ((Command) -> Unit)? = null
    private var currentDialog: AlertDialog? = null
    
    init {
        registerDefaultCommands()
    }
    
    /**
     * Register a new command.
     */
    fun registerCommand(command: Command) {
        if (commands.none { it.id == command.id }) {
            commands.add(command)
        }
    }
    
    /**
     * Unregister a command.
     */
    fun unregisterCommand(commandId: String) {
        commands.removeAll { it.id == commandId }
    }
    
    /**
     * Show the command palette dialog.
     */
    fun show(onCommand: (Command) -> Unit) {
        onCommandExecuted = onCommand
        _filteredCommands.value = getInitialCommands()
        
        val dialog = BottomSheetDialog(context, R.style.Theme_OfficeSuite_BottomSheet)
        
        // Create custom layout
        val view = createPaletteView(dialog)
        dialog.setContentView(view)
        
        currentDialog = AlertDialog.Builder(context).create()
        dialog.show()
    }
    
    /**
     * Filter commands based on query.
     */
    fun filter(query: String) {
        if (query.isBlank()) {
            _filteredCommands.value = getInitialCommands()
            return
        }
        
        val normalizedQuery = query.lowercase()
        
        _filteredCommands.value = commands
            .filter { command ->
                // Fuzzy match on name, keywords, and category
                val matchesName = fuzzyMatch(command.name.lowercase(), normalizedQuery)
                val matchesKeywords = command.keywords.any { 
                    fuzzyMatch(it.lowercase(), normalizedQuery) 
                }
                val matchesCategory = fuzzyMatch(command.category.name.lowercase(), normalizedQuery)
                
                matchesName || matchesKeywords || matchesCategory
            }
            .sortedByDescending { command ->
                // Score results by relevance
                val nameScore = if (command.name.lowercase().startsWith(normalizedQuery)) 100 else 0
                val exactMatch = if (command.name.lowercase() == normalizedQuery) 50 else 0
                val recentScore = if (recentCommands.contains(command.id)) 25 else 0
                nameScore + exactMatch + recentScore
            }
    }
    
    /**
     * Execute a command.
     */
    fun executeCommand(command: Command) {
        // Track as recent
        recentCommands.remove(command.id)
        recentCommands.add(0, command.id)
        if (recentCommands.size > 10) {
            recentCommands.removeAt(recentCommands.size - 1)
        }
        
        // Execute
        command.action()
        onCommandExecuted?.invoke(command)
        
        // Dismiss dialog
        currentDialog?.dismiss()
    }
    
    /**
     * Get all registered commands.
     */
    fun getAllCommands(): List<Command> = commands.toList()
    
    /**
     * Get commands by category.
     */
    fun getCommandsByCategory(category: CommandCategory): List<Command> {
        return commands.filter { it.category == category }
    }
    
    private fun getInitialCommands(): List<Command> {
        // Show recent commands first, then all commands
        val recent = recentCommands.mapNotNull { id -> commands.find { it.id == id } }
        val others = commands.filter { !recentCommands.contains(it.id) }
        return recent + others
    }
    
    private fun fuzzyMatch(text: String, query: String): Boolean {
        if (query.isEmpty()) return true
        
        var queryIndex = 0
        for (char in text) {
            if (queryIndex < query.length && char == query[queryIndex]) {
                queryIndex++
            }
        }
        return queryIndex == query.length || text.contains(query)
    }
    
    private fun createPaletteView(dialog: BottomSheetDialog): View {
        // Create a simple LinearLayout with EditText and RecyclerView
        val layout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        // Search EditText
        val searchEdit = EditText(context).apply {
            hint = "Type a command..."
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_search, 0, 0, 0)
            compoundDrawablePadding = 16
        }
        
        // Commands RecyclerView
        val recyclerView = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = CommandAdapter(_filteredCommands.value) { command ->
                executeCommand(command)
                dialog.dismiss()
            }
        }
        
        // Update on search
        searchEdit.setOnKeyListener { _, _, event ->
            if (event.action == KeyEvent.ACTION_UP) {
                filter(searchEdit.text.toString())
                (recyclerView.adapter as CommandAdapter).updateCommands(_filteredCommands.value)
            }
            false
        }
        
        layout.addView(searchEdit, android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ))
        
        layout.addView(recyclerView, android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            600 // Fixed height
        ))
        
        return layout
    }
    
    private fun registerDefaultCommands() {
        // File Commands
        registerCommand(Command(
            id = "file_new",
            name = "New Document",
            description = "Create a new document",
            category = CommandCategory.FILE,
            icon = R.drawable.ic_document,
            keywords = listOf("create", "new", "document", "file"),
            action = { }
        ))
        
        registerCommand(Command(
            id = "file_open",
            name = "Open File",
            description = "Open an existing file",
            category = CommandCategory.FILE,
            icon = R.drawable.ic_folder,
            keywords = listOf("open", "browse", "file"),
            action = { }
        ))
        
        registerCommand(Command(
            id = "file_save",
            name = "Save",
            description = "Save current document",
            category = CommandCategory.FILE,
            icon = R.drawable.ic_save,
            keywords = listOf("save", "store"),
            shortcut = "Ctrl+S",
            action = { }
        ))
        
        registerCommand(Command(
            id = "file_export_pdf",
            name = "Export to PDF",
            description = "Export document as PDF",
            category = CommandCategory.FILE,
            icon = R.drawable.ic_pdf,
            keywords = listOf("export", "pdf", "convert"),
            action = { }
        ))
        
        // Edit Commands
        registerCommand(Command(
            id = "edit_undo",
            name = "Undo",
            description = "Undo last action",
            category = CommandCategory.EDIT,
            icon = R.drawable.ic_undo,
            keywords = listOf("undo", "revert", "back"),
            shortcut = "Ctrl+Z",
            action = { }
        ))
        
        registerCommand(Command(
            id = "edit_redo",
            name = "Redo",
            description = "Redo last action",
            category = CommandCategory.EDIT,
            icon = R.drawable.ic_redo,
            keywords = listOf("redo", "forward"),
            shortcut = "Ctrl+Shift+Z",
            action = { }
        ))
        
        registerCommand(Command(
            id = "edit_find",
            name = "Find",
            description = "Search in document",
            category = CommandCategory.EDIT,
            icon = R.drawable.ic_search,
            keywords = listOf("find", "search", "look"),
            shortcut = "Ctrl+F",
            action = { }
        ))
        
        registerCommand(Command(
            id = "edit_replace",
            name = "Find and Replace",
            description = "Find and replace text",
            category = CommandCategory.EDIT,
            icon = R.drawable.ic_search,
            keywords = listOf("replace", "find", "substitute"),
            shortcut = "Ctrl+H",
            action = { }
        ))
        
        // View Commands
        registerCommand(Command(
            id = "view_zoom_in",
            name = "Zoom In",
            description = "Increase zoom level",
            category = CommandCategory.VIEW,
            icon = R.drawable.ic_zoom_in,
            keywords = listOf("zoom", "enlarge", "bigger"),
            shortcut = "Ctrl++",
            action = { }
        ))
        
        registerCommand(Command(
            id = "view_zoom_out",
            name = "Zoom Out",
            description = "Decrease zoom level",
            category = CommandCategory.VIEW,
            icon = R.drawable.ic_zoom_out,
            keywords = listOf("zoom", "shrink", "smaller"),
            shortcut = "Ctrl+-",
            action = { }
        ))
        
        registerCommand(Command(
            id = "view_focus_mode",
            name = "Toggle Focus Mode",
            description = "Enter/exit distraction-free writing",
            category = CommandCategory.VIEW,
            icon = R.drawable.ic_focus,
            keywords = listOf("focus", "distraction", "zen", "fullscreen"),
            action = { }
        ))
        
        registerCommand(Command(
            id = "view_dark_mode",
            name = "Toggle Dark Mode",
            description = "Switch between light and dark themes",
            category = CommandCategory.VIEW,
            icon = R.drawable.ic_dark_mode,
            keywords = listOf("dark", "light", "theme", "mode"),
            action = { }
        ))
        
        // Tools Commands
        registerCommand(Command(
            id = "tools_scanner",
            name = "Open Scanner",
            description = "Scan documents with camera",
            category = CommandCategory.TOOLS,
            icon = R.drawable.ic_scan,
            keywords = listOf("scan", "camera", "ocr"),
            action = { }
        ))
        
        registerCommand(Command(
            id = "tools_converter",
            name = "File Converter",
            description = "Convert between file formats",
            category = CommandCategory.TOOLS,
            icon = R.drawable.ic_convert,
            keywords = listOf("convert", "transform", "format"),
            action = { }
        ))
        
        registerCommand(Command(
            id = "tools_statistics",
            name = "Document Statistics",
            description = "View word count and statistics",
            category = CommandCategory.TOOLS,
            icon = R.drawable.ic_statistics,
            keywords = listOf("statistics", "word count", "analytics"),
            action = { }
        ))
        
        // Settings Commands
        registerCommand(Command(
            id = "settings_preferences",
            name = "Open Settings",
            description = "Configure app preferences",
            category = CommandCategory.SETTINGS,
            icon = R.drawable.ic_settings,
            keywords = listOf("settings", "preferences", "config"),
            action = { }
        ))
        
        registerCommand(Command(
            id = "settings_shortcuts",
            name = "Keyboard Shortcuts",
            description = "View and customize shortcuts",
            category = CommandCategory.SETTINGS,
            icon = R.drawable.ic_keyboard,
            keywords = listOf("shortcuts", "keyboard", "hotkeys"),
            action = { }
        ))
        
        // Developer Commands
        registerCommand(Command(
            id = "dev_debug_mode",
            name = "Toggle Debug Mode",
            description = "Enable/disable debug logging",
            category = CommandCategory.DEVELOPER,
            icon = R.drawable.ic_debug,
            keywords = listOf("debug", "developer", "log"),
            action = { }
        ))
        
        registerCommand(Command(
            id = "dev_performance",
            name = "Performance Monitor",
            description = "View performance metrics",
            category = CommandCategory.DEVELOPER,
            icon = R.drawable.ic_performance,
            keywords = listOf("performance", "memory", "fps"),
            action = { }
        ))
    }
    
    companion object {
        @Volatile
        private var instance: CommandPalette? = null
        
        fun getInstance(context: Context): CommandPalette {
            return instance ?: synchronized(this) {
                instance ?: CommandPalette(context.applicationContext).also { instance = it }
            }
        }
    }
}

/**
 * Command data class.
 */
data class Command(
    val id: String,
    val name: String,
    val description: String,
    val category: CommandCategory,
    val icon: Int,
    val keywords: List<String> = emptyList(),
    val shortcut: String? = null,
    val isEnabled: Boolean = true,
    val action: () -> Unit
)

/**
 * Command categories.
 */
enum class CommandCategory(val displayName: String) {
    FILE("File"),
    EDIT("Edit"),
    VIEW("View"),
    TOOLS("Tools"),
    SETTINGS("Settings"),
    DEVELOPER("Developer"),
    CUSTOM("Custom")
}

/**
 * Simple RecyclerView adapter for commands.
 */
private class CommandAdapter(
    private var commands: List<Command>,
    private val onClick: (Command) -> Unit
) : RecyclerView.Adapter<CommandAdapter.ViewHolder>() {
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView as MaterialCardView
        val icon: ImageView = itemView.findViewById(android.R.id.icon)
        val name: TextView = itemView.findViewById(android.R.id.text1)
        val description: TextView = itemView.findViewById(android.R.id.text2)
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val card = MaterialCardView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 8 }
            radius = 8f
            cardElevation = 2f
            setContentPadding(16, 12, 16, 12)
        }
        
        val layout = android.widget.LinearLayout(parent.context).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        
        val icon = ImageView(parent.context).apply {
            id = android.R.id.icon
            layoutParams = android.widget.LinearLayout.LayoutParams(48, 48).apply {
                marginEnd = 16
            }
        }
        
        val textLayout = android.widget.LinearLayout(parent.context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        
        val name = TextView(parent.context).apply {
            id = android.R.id.text1
            textSize = 16f
            setTextColor(android.graphics.Color.BLACK)
        }
        
        val description = TextView(parent.context).apply {
            id = android.R.id.text2
            textSize = 12f
            setTextColor(android.graphics.Color.GRAY)
        }
        
        textLayout.addView(name)
        textLayout.addView(description)
        layout.addView(icon)
        layout.addView(textLayout)
        card.addView(layout)
        
        return ViewHolder(card)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val command = commands[position]
        holder.icon.setImageResource(command.icon)
        holder.name.text = command.name
        holder.description.text = buildString {
            append(command.description)
            if (command.shortcut != null) {
                append(" • ")
                append(command.shortcut)
            }
        }
        holder.card.setOnClickListener { onClick(command) }
        holder.card.alpha = if (command.isEnabled) 1f else 0.5f
    }
    
    override fun getItemCount() = commands.size
    
    fun updateCommands(newCommands: List<Command>) {
        commands = newCommands
        notifyDataSetChanged()
    }
}
