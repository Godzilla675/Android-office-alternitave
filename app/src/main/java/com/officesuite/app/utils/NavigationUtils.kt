package com.officesuite.app.utils

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.officesuite.app.R
import com.officesuite.app.data.model.DocumentType

/**
 * Utility object for navigating to document viewers.
 * Centralizes navigation logic to avoid code duplication.
 */
object NavigationUtils {

    /**
     * Navigate to the appropriate viewer fragment based on document type.
     * 
     * @param fragment The source fragment
     * @param uri The document URI as string
     * @param type The document type
     */
    fun navigateToViewer(fragment: Fragment, uri: String, type: DocumentType) {
        val bundle = Bundle().apply {
            putString("file_uri", uri)
        }
        
        val navController = fragment.findNavController()
        
        when (type) {
            DocumentType.PDF -> {
                navController.navigate(R.id.pdfViewerFragment, bundle)
            }
            DocumentType.DOCX, DocumentType.DOC -> {
                navController.navigate(R.id.docxViewerFragment, bundle)
            }
            DocumentType.PPTX, DocumentType.PPT -> {
                navController.navigate(R.id.pptxViewerFragment, bundle)
            }
            DocumentType.MARKDOWN, DocumentType.TXT -> {
                navController.navigate(R.id.markdownFragment, bundle)
            }
            DocumentType.CSV -> {
                navController.navigate(R.id.csvViewerFragment, bundle)
            }
            DocumentType.JSON -> {
                navController.navigate(R.id.jsonViewerFragment, bundle)
            }
            DocumentType.XML -> {
                bundle.putBoolean("is_xml", true)
                navController.navigate(R.id.jsonViewerFragment, bundle)
            }
            else -> {
                Toast.makeText(fragment.context, "Unsupported file type", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Navigate to the appropriate viewer fragment based on document type name.
     * Useful when type is stored as a string.
     * 
     * @param fragment The source fragment
     * @param uri The document URI as string  
     * @param typeName The document type name as string
     */
    fun navigateToViewer(fragment: Fragment, uri: String, typeName: String) {
        val docType = try {
            DocumentType.valueOf(typeName)
        } catch (e: Exception) {
            DocumentType.UNKNOWN
        }
        navigateToViewer(fragment, uri, docType)
    }
}
