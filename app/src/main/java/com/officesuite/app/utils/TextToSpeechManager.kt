package com.officesuite.app.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import java.util.Locale

/**
 * Manager for Text-to-Speech functionality.
 * Implements Nice-to-Have Feature #13: Accessibility Features (Text-to-Speech)
 */
class TextToSpeechManager(
    private val context: Context,
    private val onInitialized: (Boolean) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var onCompletionListener: (() -> Unit)? = null
    private var onProgressListener: ((String, Int, Int) -> Unit)? = null

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.getDefault())
            isInitialized = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    // Speech started
                }

                override fun onDone(utteranceId: String?) {
                    onCompletionListener?.invoke()
                }

                override fun onError(utteranceId: String?) {
                    // Error occurred
                }
            })
            
            onInitialized(isInitialized)
        } else {
            isInitialized = false
            onInitialized(false)
        }
    }

    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (!isInitialized) {
            Toast.makeText(context, "Text-to-Speech not available", Toast.LENGTH_SHORT).show()
            return
        }
        
        tts?.speak(text, queueMode, null, "utterance_${System.currentTimeMillis()}")
    }

    fun speakParagraphs(paragraphs: List<String>, onComplete: (() -> Unit)? = null) {
        if (!isInitialized) {
            Toast.makeText(context, "Text-to-Speech not available", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (paragraphs.isEmpty()) {
            onComplete?.invoke()
            return
        }
        
        onCompletionListener = onComplete
        
        paragraphs.forEachIndexed { index, paragraph ->
            val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val utteranceId = if (index == paragraphs.lastIndex) "last_utterance" else "utterance_$index"
            tts?.speak(paragraph, queueMode, null, utteranceId)
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun pause() {
        // TTS doesn't have a native pause, so we stop and track position
        tts?.stop()
    }

    fun isSpeaking(): Boolean {
        return tts?.isSpeaking == true
    }

    fun setSpeed(speed: Float) {
        tts?.setSpeechRate(speed.coerceIn(0.1f, 2.0f))
    }

    fun setPitch(pitch: Float) {
        tts?.setPitch(pitch.coerceIn(0.1f, 2.0f))
    }

    fun setLanguage(locale: Locale): Boolean {
        val result = tts?.setLanguage(locale)
        return result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    fun getAvailableLanguages(): List<Locale> {
        return tts?.availableLanguages?.toList() ?: emptyList()
    }

    fun setOnCompletionListener(listener: (() -> Unit)?) {
        onCompletionListener = listener
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    fun isAvailable(): Boolean = isInitialized
}
