package com.officesuite.app.platform

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for VoiceCommandManager class.
 */
class VoiceCommandManagerTest {
    
    @Test
    fun `VoiceCommand enum has correct values`() {
        val commands = VoiceCommandManager.VoiceCommand.entries
        assertEquals(16, commands.size)
    }
    
    @Test
    fun `VoiceCommand OPEN_DOCUMENT has correct properties`() {
        val command = VoiceCommandManager.VoiceCommand.OPEN_DOCUMENT
        assertEquals("open_document", command.id)
        assertTrue(command.phrases.contains("open"))
        assertTrue(command.phrases.contains("open document"))
        assertEquals("Open a document", command.description)
    }
    
    @Test
    fun `VoiceCommand SCAN_DOCUMENT has correct properties`() {
        val command = VoiceCommandManager.VoiceCommand.SCAN_DOCUMENT
        assertEquals("scan_document", command.id)
        assertTrue(command.phrases.contains("scan"))
        assertTrue(command.phrases.contains("scan document"))
        assertEquals("Scan a document", command.description)
    }
    
    @Test
    fun `VoiceCommand READ_ALOUD has correct properties`() {
        val command = VoiceCommandManager.VoiceCommand.READ_ALOUD
        assertEquals("read_aloud", command.id)
        assertTrue(command.phrases.contains("read"))
        assertTrue(command.phrases.contains("read aloud"))
        assertEquals("Read document aloud", command.description)
    }
    
    @Test
    fun `VoiceCommand ZOOM_IN has correct properties`() {
        val command = VoiceCommandManager.VoiceCommand.ZOOM_IN
        assertEquals("zoom_in", command.id)
        assertTrue(command.phrases.contains("zoom in"))
        assertTrue(command.phrases.contains("bigger"))
        assertEquals("Zoom in", command.description)
    }
    
    @Test
    fun `VoiceCommand GO_TO_PAGE has correct properties`() {
        val command = VoiceCommandManager.VoiceCommand.GO_TO_PAGE
        assertEquals("go_to_page", command.id)
        assertTrue(command.phrases.contains("go to page"))
        assertEquals("Go to specific page", command.description)
    }
    
    @Test
    fun `VoiceCommandResult data class holds values correctly`() {
        val result = VoiceCommandManager.VoiceCommandResult(
            command = VoiceCommandManager.VoiceCommand.OPEN_DOCUMENT,
            parameter = "test.pdf",
            confidence = 0.95f,
            rawText = "open document test.pdf"
        )
        
        assertEquals(VoiceCommandManager.VoiceCommand.OPEN_DOCUMENT, result.command)
        assertEquals("test.pdf", result.parameter)
        assertEquals(0.95f, result.confidence, 0.01f)
        assertEquals("open document test.pdf", result.rawText)
    }
    
    @Test
    fun `VoiceCommandResult with null command`() {
        val result = VoiceCommandManager.VoiceCommandResult(
            command = null,
            parameter = null,
            confidence = 0.0f,
            rawText = "unknown command"
        )
        
        assertNull(result.command)
        assertNull(result.parameter)
        assertEquals(0.0f, result.confidence, 0.01f)
        assertEquals("unknown command", result.rawText)
    }
    
    @Test
    fun `VoiceSettings data class holds values correctly`() {
        val settings = VoiceCommandManager.VoiceSettings(
            isEnabled = true,
            language = "en",
            confirmBeforeAction = true,
            readbackEnabled = true,
            continuousListening = false,
            customWakeWord = "Hey Office"
        )
        
        assertTrue(settings.isEnabled)
        assertEquals("en", settings.language)
        assertTrue(settings.confirmBeforeAction)
        assertTrue(settings.readbackEnabled)
        assertFalse(settings.continuousListening)
        assertEquals("Hey Office", settings.customWakeWord)
    }
    
    @Test
    fun `VoiceSettings default values are correct`() {
        val defaultSettings = VoiceCommandManager.VoiceSettings()
        
        assertTrue(defaultSettings.isEnabled)
        assertTrue(defaultSettings.confirmBeforeAction)
        assertTrue(defaultSettings.readbackEnabled)
        assertFalse(defaultSettings.continuousListening)
        assertNull(defaultSettings.customWakeWord)
    }
    
    @Test
    fun `All VoiceCommands have unique IDs`() {
        val commands = VoiceCommandManager.VoiceCommand.entries
        val ids = commands.map { it.id }
        val uniqueIds = ids.toSet()
        assertEquals(ids.size, uniqueIds.size)
    }
    
    @Test
    fun `All VoiceCommands have non-empty phrases`() {
        val commands = VoiceCommandManager.VoiceCommand.entries
        commands.forEach { command ->
            assertTrue("Command ${command.id} has empty phrases", command.phrases.isNotEmpty())
        }
    }
    
    @Test
    fun `All VoiceCommands have non-empty descriptions`() {
        val commands = VoiceCommandManager.VoiceCommand.entries
        commands.forEach { command ->
            assertTrue("Command ${command.id} has empty description", command.description.isNotEmpty())
        }
    }
}
