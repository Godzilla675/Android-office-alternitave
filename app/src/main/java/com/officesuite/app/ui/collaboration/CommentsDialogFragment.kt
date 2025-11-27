package com.officesuite.app.ui.collaboration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.officesuite.app.R
import com.officesuite.app.data.collaboration.CollaborationRepository
import com.officesuite.app.data.collaboration.DocumentComment
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog fragment for displaying and adding comments to a document
 */
class CommentsDialogFragment : DialogFragment() {

    private lateinit var collaborationRepository: CollaborationRepository
    private lateinit var commentsAdapter: CommentsAdapter
    private var documentId: String = ""
    private var pageNumber: Int? = null

    companion object {
        private const val ARG_DOCUMENT_ID = "document_id"
        private const val ARG_PAGE_NUMBER = "page_number"

        fun newInstance(documentId: String, pageNumber: Int? = null): CommentsDialogFragment {
            return CommentsDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_DOCUMENT_ID, documentId)
                    pageNumber?.let { putInt(ARG_PAGE_NUMBER, it) }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.ThemeOverlay_MaterialComponents_Dialog)
        documentId = arguments?.getString(ARG_DOCUMENT_ID) ?: ""
        pageNumber = arguments?.getInt(ARG_PAGE_NUMBER)?.takeIf { it > 0 }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_comments, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        collaborationRepository = CollaborationRepository(requireContext())
        
        setupViews(view)
        loadComments()
    }

    private fun setupViews(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerComments)
        val editComment = view.findViewById<TextInputEditText>(R.id.editComment)
        val btnAddComment = view.findViewById<MaterialButton>(R.id.btnAddComment)
        val btnClose = view.findViewById<MaterialButton>(R.id.btnClose)

        commentsAdapter = CommentsAdapter(
            onReplyClick = { comment -> showReplyDialog(comment) },
            onResolveClick = { comment -> resolveComment(comment) },
            onDeleteClick = { comment -> deleteComment(comment) }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = commentsAdapter
        }

        btnAddComment.setOnClickListener {
            val text = editComment.text?.toString()?.trim()
            if (!text.isNullOrEmpty()) {
                addComment(text)
                editComment.text?.clear()
            }
        }

        btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun loadComments() {
        lifecycleScope.launch {
            val comments = if (pageNumber != null) {
                collaborationRepository.getCommentsForPage(documentId, pageNumber!!)
            } else {
                collaborationRepository.getCommentsForDocument(documentId)
            }
            
            // Organize comments into threads
            val rootComments = comments.filter { it.parentId == null }
            val threads = rootComments.map { root ->
                CommentThread(
                    root = root,
                    replies = comments.filter { it.parentId == root.id }
                )
            }.sortedByDescending { it.root.createdAt }
            
            commentsAdapter.submitList(threads)
        }
    }

    private fun addComment(text: String) {
        lifecycleScope.launch {
            val comment = DocumentComment(
                documentId = documentId,
                text = text,
                author = collaborationRepository.getCurrentUser(),
                pageNumber = pageNumber
            )
            
            val success = collaborationRepository.addComment(comment)
            if (success) {
                loadComments()
                Toast.makeText(context, "Comment added", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to add comment", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showReplyDialog(comment: DocumentComment) {
        val editText = EditText(requireContext()).apply {
            hint = "Write a reply..."
        }
        
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Reply to ${comment.author}")
            .setView(editText)
            .setPositiveButton("Reply") { _, _ ->
                val replyText = editText.text.toString().trim()
                if (replyText.isNotEmpty()) {
                    replyToComment(comment.id, replyText)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun replyToComment(parentId: String, text: String) {
        lifecycleScope.launch {
            val success = collaborationRepository.replyToComment(parentId, text)
            if (success) {
                loadComments()
            }
        }
    }

    private fun resolveComment(comment: DocumentComment) {
        lifecycleScope.launch {
            val success = collaborationRepository.resolveComment(comment.id, !comment.resolved)
            if (success) {
                loadComments()
            }
        }
    }

    private fun deleteComment(comment: DocumentComment) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Comment")
            .setMessage("Are you sure you want to delete this comment?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    val success = collaborationRepository.deleteComment(comment.id)
                    if (success) {
                        loadComments()
                        Toast.makeText(context, "Comment deleted", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

/**
 * Represents a comment thread with root comment and replies
 */
data class CommentThread(
    val root: DocumentComment,
    val replies: List<DocumentComment>
)

/**
 * Adapter for displaying comments
 */
class CommentsAdapter(
    private val onReplyClick: (DocumentComment) -> Unit,
    private val onResolveClick: (DocumentComment) -> Unit,
    private val onDeleteClick: (DocumentComment) -> Unit
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    private var threads = listOf<CommentThread>()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    fun submitList(newThreads: List<CommentThread>) {
        threads = newThreads
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(threads[position])
    }

    override fun getItemCount() = threads.size

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textAuthor = itemView.findViewById<android.widget.TextView>(R.id.textAuthor)
        private val textDate = itemView.findViewById<android.widget.TextView>(R.id.textDate)
        private val textComment = itemView.findViewById<android.widget.TextView>(R.id.textComment)
        private val textResolved = itemView.findViewById<android.widget.TextView>(R.id.textResolved)
        private val layoutReplies = itemView.findViewById<android.widget.LinearLayout>(R.id.layoutReplies)
        private val btnReply = itemView.findViewById<MaterialButton>(R.id.btnReply)
        private val btnResolve = itemView.findViewById<MaterialButton>(R.id.btnResolve)
        private val btnDelete = itemView.findViewById<MaterialButton>(R.id.btnDelete)

        fun bind(thread: CommentThread) {
            val comment = thread.root
            
            textAuthor.text = comment.author
            textDate.text = dateFormat.format(Date(comment.createdAt))
            textComment.text = comment.text
            
            if (comment.resolved) {
                textResolved.visibility = View.VISIBLE
                btnResolve.text = "Unresolve"
            } else {
                textResolved.visibility = View.GONE
                btnResolve.text = "Resolve"
            }
            
            // Show replies
            layoutReplies.removeAllViews()
            for (reply in thread.replies) {
                val replyView = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_comment_reply, layoutReplies, false)
                
                replyView.findViewById<android.widget.TextView>(R.id.textReplyAuthor).text = reply.author
                replyView.findViewById<android.widget.TextView>(R.id.textReplyDate).text = 
                    dateFormat.format(Date(reply.createdAt))
                replyView.findViewById<android.widget.TextView>(R.id.textReplyContent).text = reply.text
                
                layoutReplies.addView(replyView)
            }
            
            layoutReplies.visibility = if (thread.replies.isEmpty()) View.GONE else View.VISIBLE
            
            btnReply.setOnClickListener { onReplyClick(comment) }
            btnResolve.setOnClickListener { onResolveClick(comment) }
            btnDelete.setOnClickListener { onDeleteClick(comment) }
        }
    }
}
