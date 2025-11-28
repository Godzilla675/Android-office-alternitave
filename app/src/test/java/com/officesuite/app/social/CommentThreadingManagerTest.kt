package com.officesuite.app.social

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for CommentThreadingManager class.
 */
class CommentThreadingManagerTest {
    
    @Test
    fun `Comment data class holds values correctly`() {
        val comment = CommentThreadingManager.Comment(
            id = "test-id",
            documentUri = "content://doc/1",
            parentId = null,
            author = "John Doe",
            authorId = "user-1",
            text = "This is a test comment",
            mentions = listOf("user-2", "user-3"),
            pageNumber = 5,
            x = 100f,
            y = 200f
        )
        
        assertEquals("test-id", comment.id)
        assertEquals("content://doc/1", comment.documentUri)
        assertNull(comment.parentId)
        assertEquals("John Doe", comment.author)
        assertEquals("user-1", comment.authorId)
        assertEquals("This is a test comment", comment.text)
        assertEquals(2, comment.mentions.size)
        assertEquals(5, comment.pageNumber)
        assertEquals(100f, comment.x!!, 0.01f)
        assertEquals(200f, comment.y!!, 0.01f)
        assertFalse(comment.isReply())
    }
    
    @Test
    fun `Comment isReply returns true for replies`() {
        val reply = CommentThreadingManager.Comment(
            id = "reply-id",
            documentUri = "content://doc/1",
            parentId = "parent-id",
            author = "Jane Doe",
            authorId = "user-2",
            text = "This is a reply"
        )
        
        assertTrue(reply.isReply())
    }
    
    @Test
    fun `Comment isReply returns false for parent comments`() {
        val parent = CommentThreadingManager.Comment(
            id = "parent-id",
            documentUri = "content://doc/1",
            parentId = null,
            author = "John Doe",
            authorId = "user-1",
            text = "This is a parent comment"
        )
        
        assertFalse(parent.isReply())
    }
    
    @Test
    fun `CommentThread totalComments includes parent and replies`() {
        val parent = CommentThreadingManager.Comment(
            id = "parent-id",
            documentUri = "content://doc/1",
            author = "John Doe",
            authorId = "user-1",
            text = "Parent comment"
        )
        
        val replies = listOf(
            CommentThreadingManager.Comment(
                id = "reply-1",
                documentUri = "content://doc/1",
                parentId = "parent-id",
                author = "Jane Doe",
                authorId = "user-2",
                text = "Reply 1"
            ),
            CommentThreadingManager.Comment(
                id = "reply-2",
                documentUri = "content://doc/1",
                parentId = "parent-id",
                author = "Bob Smith",
                authorId = "user-3",
                text = "Reply 2"
            )
        )
        
        val thread = CommentThreadingManager.CommentThread(parent, replies)
        
        assertEquals(3, thread.totalComments())
    }
    
    @Test
    fun `CommentThread uniqueAuthors returns all unique authors`() {
        val parent = CommentThreadingManager.Comment(
            id = "parent-id",
            documentUri = "content://doc/1",
            author = "John Doe",
            authorId = "user-1",
            text = "Parent comment"
        )
        
        val replies = listOf(
            CommentThreadingManager.Comment(
                id = "reply-1",
                documentUri = "content://doc/1",
                parentId = "parent-id",
                author = "Jane Doe",
                authorId = "user-2",
                text = "Reply 1"
            ),
            CommentThreadingManager.Comment(
                id = "reply-2",
                documentUri = "content://doc/1",
                parentId = "parent-id",
                author = "John Doe",
                authorId = "user-1",  // Same as parent
                text = "Reply 2"
            )
        )
        
        val thread = CommentThreadingManager.CommentThread(parent, replies)
        
        assertEquals(2, thread.uniqueAuthors().size)
        assertTrue(thread.uniqueAuthors().contains("user-1"))
        assertTrue(thread.uniqueAuthors().contains("user-2"))
    }
    
    @Test
    fun `MentionNotification data class holds values correctly`() {
        val notification = CommentThreadingManager.MentionNotification(
            id = "mention-id",
            commentId = "comment-id",
            documentUri = "content://doc/1",
            mentionedUserId = "user-2",
            mentionedByUserId = "user-1",
            mentionedByName = "John Doe",
            commentPreview = "Hey @user-2 check this out!",
            isRead = false
        )
        
        assertEquals("mention-id", notification.id)
        assertEquals("comment-id", notification.commentId)
        assertEquals("content://doc/1", notification.documentUri)
        assertEquals("user-2", notification.mentionedUserId)
        assertEquals("user-1", notification.mentionedByUserId)
        assertEquals("John Doe", notification.mentionedByName)
        assertFalse(notification.isRead)
    }
    
    @Test
    fun `User data class holds values correctly`() {
        val user = CommentThreadingManager.User(
            id = "user-1",
            name = "johndoe",
            displayName = "John Doe",
            avatarUrl = "https://example.com/avatar.jpg"
        )
        
        assertEquals("user-1", user.id)
        assertEquals("johndoe", user.name)
        assertEquals("John Doe", user.displayName)
        assertEquals("https://example.com/avatar.jpg", user.avatarUrl)
    }
    
    @Test
    fun `extractMentions extracts simple mentions`() {
        val text = "Hey @john and @jane, please review this."
        val mentions = CommentThreadingManager.extractMentions(text)
        
        assertEquals(2, mentions.size)
        assertTrue(mentions.contains("john"))
        assertTrue(mentions.contains("jane"))
    }
    
    @Test
    fun `extractMentions extracts bracket mentions`() {
        val text = "Hey @[John Doe] and @[Jane Smith], please review this."
        val mentions = CommentThreadingManager.extractMentions(text)
        
        assertEquals(2, mentions.size)
        assertTrue(mentions.contains("John Doe"))
        assertTrue(mentions.contains("Jane Smith"))
    }
    
    @Test
    fun `extractMentions handles mixed mention formats`() {
        val text = "Hey @john and @[Jane Smith], please review."
        val mentions = CommentThreadingManager.extractMentions(text)
        
        assertEquals(2, mentions.size)
        assertTrue(mentions.contains("john"))
        assertTrue(mentions.contains("Jane Smith"))
    }
    
    @Test
    fun `extractMentions returns empty list when no mentions`() {
        val text = "This is a comment without any mentions."
        val mentions = CommentThreadingManager.extractMentions(text)
        
        assertTrue(mentions.isEmpty())
    }
    
    @Test
    fun `formatMentions wraps mentions in tags`() {
        val text = "Hey @john, please review."
        val formatted = CommentThreadingManager.formatMentions(text)
        
        assertTrue(formatted.contains("<mention>@john</mention>"))
    }
}
