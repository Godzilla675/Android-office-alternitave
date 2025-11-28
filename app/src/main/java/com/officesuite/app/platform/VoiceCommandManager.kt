package com.officesuite.app.platform

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * Manages Google Assistant integration for voice commands.
 * Implements Phase 2 Platform-Specific Feature #25: Google Assistant Actions
 * 
 * Enables users to interact with the app using voice commands through
 * Google Assistant.
 */
class VoiceCommandManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    /**
     * Available voice commands
     */
    enum class VoiceCommand(
        val id: String,
        val phrases: List<String>,
        val description: String
    ) {
        OPEN_DOCUMENT(
            "open_document",
            listOf("open", "open document", "open file"),
            "Open a document"
        ),
        SCAN_DOCUMENT(
            "scan_document",
            listOf("scan", "scan document", "take a scan", "scan page"),
            "Scan a document"
        ),
        CREATE_DOCUMENT(
            "create_document",
            listOf("create", "create document", "new document", "new file"),
            "Create a new document"
        ),
        SEARCH(
            "search",
            listOf("search", "find", "search for", "look for"),
            "Search documents"
        ),
        CONVERT(
            "convert",
            listOf("convert", "convert to pdf", "convert document"),
            "Convert a document"
        ),
        READ_ALOUD(
            "read_aloud",
            listOf("read", "read aloud", "read this", "read document"),
            "Read document aloud"
        ),
        STOP_READING(
            "stop_reading",
            listOf("stop", "stop reading", "stop speaking"),
            "Stop reading aloud"
        ),
        NEXT_PAGE(
            "next_page",
            listOf("next", "next page", "go to next page"),
            "Go to next page"
        ),
        PREVIOUS_PAGE(
            "previous_page",
            listOf("previous", "previous page", "go back"),
            "Go to previous page"
        ),
        GO_TO_PAGE(
            "go_to_page",
            listOf("go to page", "page number", "jump to page"),
            "Go to specific page"
        ),
        ZOOM_IN(
            "zoom_in",
            listOf("zoom in", "bigger", "make bigger", "enlarge"),
            "Zoom in"
        ),
        ZOOM_OUT(
            "zoom_out",
            listOf("zoom out", "smaller", "make smaller"),
            "Zoom out"
        ),
        SHARE(
            "share",
            listOf("share", "share document", "send"),
            "Share document"
        ),
        PRINT(
            "print",
            listOf("print", "print document", "print this"),
            "Print document"
        ),
        OPEN_RECENT(
            "open_recent",
            listOf("open recent", "recent documents", "last document"),
            "Open recent documents"
        ),
        HELP(
            "help",
            listOf("help", "what can you do", "voice commands"),
            "Show help"
        )
    }
    
    /**
     * Voice command result
     */
    data class VoiceCommandResult(
        val command: VoiceCommand?,
        val parameter: String?,
        val confidence: Float,
        val rawText: String
    )
    
    /**
     * Voice settings
     */
    data class VoiceSettings(
        val isEnabled: Boolean = true,
        val language: String = Locale.getDefault().language,
        val confirmBeforeAction: Boolean = true,
        val readbackEnabled: Boolean = true,
        val continuousListening: Boolean = false,
        val customWakeWord: String? = null
    )
    
    /**
     * Check if voice recognition is available
     */
    fun isVoiceRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }
    
    /**
     * Create speech recognizer intent
     */
    fun createRecognizerIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, getVoiceSettings().language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Say a command (e.g., 'scan document', 'open file')"
            )
        }
    }
    
    /**
     * Parse voice input to command
     */
    fun parseVoiceInput(input: String): VoiceCommandResult {
        val lowerInput = input.lowercase(Locale.getDefault()).trim()
        
        // Find matching command
        var bestMatch: VoiceCommand? = null
        var bestConfidence = 0f
        var parameter: String? = null
        
        for (command in VoiceCommand.entries) {
            for (phrase in command.phrases) {
                if (lowerInput.startsWith(phrase)) {
                    val confidence = phrase.length.toFloat() / lowerInput.length
                    if (confidence > bestConfidence) {
                        bestConfidence = confidence
                        bestMatch = command
                        // Extract parameter after the phrase
                        parameter = lowerInput.removePrefix(phrase).trim()
                            .takeIf { it.isNotEmpty() }
                    }
                } else if (lowerInput.contains(phrase)) {
                    val confidence = 0.5f * (phrase.length.toFloat() / lowerInput.length)
                    if (confidence > bestConfidence) {
                        bestConfidence = confidence
                        bestMatch = command
                    }
                }
            }
        }
        
        // Handle "go to page" with number
        if (bestMatch == VoiceCommand.GO_TO_PAGE) {
            val pageNumber = extractPageNumber(lowerInput)
            if (pageNumber != null) {
                parameter = pageNumber.toString()
            }
        }
        
        return VoiceCommandResult(
            command = bestMatch,
            parameter = parameter,
            confidence = bestConfidence,
            rawText = input
        )
    }
    
    /**
     * Extract page number from input
     */
    private fun extractPageNumber(input: String): Int? {
        val numberWords = mapOf(
            "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
            "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
            "first" to 1, "second" to 2, "third" to 3, "fourth" to 4, "fifth" to 5,
            "last" to -1
        )
        
        // Try to find number word
        for ((word, num) in numberWords) {
            if (input.contains(word)) {
                return num
            }
        }
        
        // Try to find digits
        val regex = Regex("\\d+")
        return regex.find(input)?.value?.toIntOrNull()
    }
    
    /**
     * Get voice settings
     */
    fun getVoiceSettings(): VoiceSettings {
        return VoiceSettings(
            isEnabled = prefs.getBoolean(KEY_VOICE_ENABLED, true),
            language = prefs.getString(KEY_VOICE_LANGUAGE, Locale.getDefault().language) 
                ?: Locale.getDefault().language,
            confirmBeforeAction = prefs.getBoolean(KEY_CONFIRM_ACTION, true),
            readbackEnabled = prefs.getBoolean(KEY_READBACK, true),
            continuousListening = prefs.getBoolean(KEY_CONTINUOUS, false),
            customWakeWord = prefs.getString(KEY_WAKE_WORD, null)
        )
    }
    
    /**
     * Save voice settings
     */
    fun saveVoiceSettings(settings: VoiceSettings) {
        prefs.edit().apply {
            putBoolean(KEY_VOICE_ENABLED, settings.isEnabled)
            putString(KEY_VOICE_LANGUAGE, settings.language)
            putBoolean(KEY_CONFIRM_ACTION, settings.confirmBeforeAction)
            putBoolean(KEY_READBACK, settings.readbackEnabled)
            putBoolean(KEY_CONTINUOUS, settings.continuousListening)
            if (settings.customWakeWord != null) {
                putString(KEY_WAKE_WORD, settings.customWakeWord)
            } else {
                remove(KEY_WAKE_WORD)
            }
            apply()
        }
    }
    
    /**
     * Get all available commands
     */
    fun getAvailableCommands(): List<VoiceCommand> {
        return VoiceCommand.entries.toList()
    }
    
    /**
     * Get command help text
     */
    fun getHelpText(): String {
        return buildString {
            appendLine("Available voice commands:")
            appendLine()
            for (command in VoiceCommand.entries) {
                appendLine("• ${command.description}")
                appendLine("  Say: ${command.phrases.joinToString(" or ")}")
                appendLine()
            }
        }
    }
    
    /**
     * Check if voice is enabled
     */
    fun isVoiceEnabled(): Boolean {
        return prefs.getBoolean(KEY_VOICE_ENABLED, true)
    }
    
    /**
     * Enable or disable voice commands
     */
    fun setVoiceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VOICE_ENABLED, enabled).apply()
    }
    
    /**
     * Get supported languages for voice
     */
    fun getSupportedLanguages(): List<Pair<String, String>> {
        return listOf(
            "en" to "English",
            "es" to "Spanish",
            "fr" to "French",
            "de" to "German",
            "it" to "Italian",
            "pt" to "Portuguese",
            "ja" to "Japanese",
            "ko" to "Korean",
            "zh" to "Chinese",
            "hi" to "Hindi",
            "ar" to "Arabic"
        )
    }
    
    /**
     * Create App Actions intent for Google Assistant
     */
    fun createAppActionIntent(action: String, extras: Bundle? = null): Intent {
        return Intent(action).apply {
            setPackage(context.packageName)
            extras?.let { putExtras(it) }
        }
    }
    
    companion object {
        private const val PREFS_NAME = "voice_command_prefs"
        private const val KEY_VOICE_ENABLED = "voice_enabled"
        private const val KEY_VOICE_LANGUAGE = "voice_language"
        private const val KEY_CONFIRM_ACTION = "confirm_action"
        private const val KEY_READBACK = "readback"
        private const val KEY_CONTINUOUS = "continuous_listening"
        private const val KEY_WAKE_WORD = "wake_word"
        
        // App Action intents
        const val ACTION_OPEN = "com.officesuite.app.intent.action.OPEN"
        const val ACTION_SCAN = "com.officesuite.app.intent.action.SCAN"
        const val ACTION_CREATE = "com.officesuite.app.intent.action.CREATE"
        const val ACTION_SEARCH = "com.officesuite.app.intent.action.SEARCH"
    }
}
