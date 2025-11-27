package com.officesuite.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.officesuite.app.R
import com.officesuite.app.data.model.DocumentFile
import com.officesuite.app.data.model.DocumentType
import com.officesuite.app.databinding.ItemRecentFileBinding
import com.officesuite.app.utils.FileUtils
import java.text.SimpleDateFormat
import java.util.*

class RecentFilesAdapter(
    private var files: List<DocumentFile>,
    private val onFileClick: (DocumentFile) -> Unit
) : RecyclerView.Adapter<RecentFilesAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentFileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(files[position])
    }

    override fun getItemCount(): Int = files.size

    fun updateFiles(newFiles: List<DocumentFile>) {
        files = newFiles
        notifyDataSetChanged()
    }

    inner class ViewHolder(
        private val binding: ItemRecentFileBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(file: DocumentFile) {
            binding.textFileName.text = file.name
            binding.textFileSize.text = FileUtils.formatFileSize(file.size)
            binding.textFileDate.text = dateFormat.format(Date(file.lastModified))
            
            val iconRes = when (file.type) {
                DocumentType.PDF -> R.drawable.ic_pdf
                DocumentType.DOCX, DocumentType.DOC -> R.drawable.ic_document
                DocumentType.PPTX, DocumentType.PPT -> R.drawable.ic_presentation
                DocumentType.MARKDOWN, DocumentType.TXT -> R.drawable.ic_text
                else -> R.drawable.ic_document
            }
            binding.imageFileIcon.setImageResource(iconRes)
            
            binding.root.setOnClickListener { onFileClick(file) }
        }
    }
}
