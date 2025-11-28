package com.officesuite.app.social

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for DocumentReactionsManager class.
 */
class DocumentReactionsManagerTest {
    
    @Test
    fun `ReactionType enum has correct emoji values`() {
        assertEquals("👍", DocumentReactionsManager.ReactionType.LIKE.emoji)
        assertEquals("❤️", DocumentReactionsManager.ReactionType.LOVE.emoji)
        assertEquals("🎉", DocumentReactionsManager.ReactionType.CELEBRATE.emoji)
        assertEquals("💡", DocumentReactionsManager.ReactionType.INSIGHTFUL.emoji)
        assertEquals("🤔", DocumentReactionsManager.ReactionType.CURIOUS.emoji)
        assertEquals("👏", DocumentReactionsManager.ReactionType.HELPFUL.emoji)
        assertEquals("⭐", DocumentReactionsManager.ReactionType.STAR.emoji)
        assertEquals("🔥", DocumentReactionsManager.ReactionType.FIRE.emoji)
        assertEquals("✅", DocumentReactionsManager.ReactionType.CHECK.emoji)
        assertEquals("👀", DocumentReactionsManager.ReactionType.EYES.emoji)
    }
    
    @Test
    fun `ReactionType enum has correct display names`() {
        assertEquals("Like", DocumentReactionsManager.ReactionType.LIKE.displayName)
        assertEquals("Love", DocumentReactionsManager.ReactionType.LOVE.displayName)
        assertEquals("Celebrate", DocumentReactionsManager.ReactionType.CELEBRATE.displayName)
        assertEquals("Insightful", DocumentReactionsManager.ReactionType.INSIGHTFUL.displayName)
        assertEquals("Curious", DocumentReactionsManager.ReactionType.CURIOUS.displayName)
        assertEquals("Helpful", DocumentReactionsManager.ReactionType.HELPFUL.displayName)
        assertEquals("Star", DocumentReactionsManager.ReactionType.STAR.displayName)
        assertEquals("Fire", DocumentReactionsManager.ReactionType.FIRE.displayName)
        assertEquals("Approved", DocumentReactionsManager.ReactionType.CHECK.displayName)
        assertEquals("Watching", DocumentReactionsManager.ReactionType.EYES.displayName)
    }
    
    @Test
    fun `ReactionType enum has all expected values`() {
        val types = DocumentReactionsManager.ReactionType.entries
        assertEquals(10, types.size)
    }
    
    @Test
    fun `DocumentReactions data class holds values correctly`() {
        val reactions = DocumentReactionsManager.DocumentReactions(
            documentUri = "content://doc/1",
            reactions = mapOf(
                "LIKE" to 5,
                "LOVE" to 3
            ),
            userReactions = setOf("LIKE"),
            lastReactionTime = 1704067200000
        )
        
        assertEquals("content://doc/1", reactions.documentUri)
        assertEquals(8, reactions.totalReactions())
        assertEquals(5, reactions.getReactionCount(DocumentReactionsManager.ReactionType.LIKE))
        assertEquals(3, reactions.getReactionCount(DocumentReactionsManager.ReactionType.LOVE))
        assertEquals(0, reactions.getReactionCount(DocumentReactionsManager.ReactionType.CELEBRATE))
        assertTrue(reactions.hasUserReacted(DocumentReactionsManager.ReactionType.LIKE))
        assertFalse(reactions.hasUserReacted(DocumentReactionsManager.ReactionType.LOVE))
    }
    
    @Test
    fun `DocumentReactions mostPopularReaction returns correct type`() {
        val reactions = DocumentReactionsManager.DocumentReactions(
            documentUri = "content://doc/1",
            reactions = mapOf(
                "LIKE" to 5,
                "LOVE" to 10,
                "CELEBRATE" to 3
            )
        )
        
        assertEquals(DocumentReactionsManager.ReactionType.LOVE, reactions.mostPopularReaction())
    }
    
    @Test
    fun `DocumentReactions with empty reactions returns null for mostPopular`() {
        val reactions = DocumentReactionsManager.DocumentReactions(
            documentUri = "content://doc/1"
        )
        
        assertNull(reactions.mostPopularReaction())
    }
    
    @Test
    fun `ReactionSummary data class holds values correctly`() {
        val summary = DocumentReactionsManager.ReactionSummary(
            topReactions = listOf(
                DocumentReactionsManager.ReactionType.LIKE,
                DocumentReactionsManager.ReactionType.LOVE
            ),
            totalCount = 15,
            userHasReacted = true
        )
        
        assertEquals(2, summary.topReactions.size)
        assertEquals(15, summary.totalCount)
        assertTrue(summary.userHasReacted)
    }
    
    @Test
    fun `getAllReactionTypes returns all types`() {
        val types = DocumentReactionsManager.getAllReactionTypes()
        assertEquals(10, types.size)
        assertTrue(types.contains(DocumentReactionsManager.ReactionType.LIKE))
        assertTrue(types.contains(DocumentReactionsManager.ReactionType.LOVE))
    }
}
