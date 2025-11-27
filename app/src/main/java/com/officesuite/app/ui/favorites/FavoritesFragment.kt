package com.officesuite.app.ui.favorites

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.officesuite.app.R
import com.officesuite.app.data.model.DocumentType
import com.officesuite.app.data.repository.FavoriteItem
import com.officesuite.app.data.repository.PreferencesRepository
import com.officesuite.app.databinding.FragmentFavoritesBinding

/**
 * Fragment for displaying and managing favorite documents.
 * Implements Nice-to-Have Feature #11: File Management Enhancements (Favorites)
 */
class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var prefsRepository: PreferencesRepository
    private lateinit var adapter: FavoritesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        prefsRepository = PreferencesRepository(requireContext())
        
        setupToolbar()
        setupRecyclerView()
        loadFavorites()
    }

    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.favorites)
    }

    private fun setupRecyclerView() {
        adapter = FavoritesAdapter(
            onItemClick = { favorite -> openDocument(favorite) },
            onRemoveClick = { favorite -> removeFavorite(favorite) }
        )
        
        binding.recyclerFavorites.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@FavoritesFragment.adapter
        }
    }

    private fun loadFavorites() {
        val favorites = prefsRepository.getFavorites()
        
        if (favorites.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.recyclerFavorites.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.recyclerFavorites.visibility = View.VISIBLE
            adapter.submitList(favorites)
        }
    }

    private fun openDocument(favorite: FavoriteItem) {
        val bundle = Bundle().apply {
            putString("file_uri", favorite.uri)
        }
        
        val docType = try {
            DocumentType.valueOf(favorite.type)
        } catch (e: Exception) {
            DocumentType.UNKNOWN
        }
        
        when (docType) {
            DocumentType.PDF -> {
                findNavController().navigate(R.id.pdfViewerFragment, bundle)
            }
            DocumentType.DOCX, DocumentType.DOC -> {
                findNavController().navigate(R.id.docxViewerFragment, bundle)
            }
            DocumentType.PPTX, DocumentType.PPT -> {
                findNavController().navigate(R.id.pptxViewerFragment, bundle)
            }
            DocumentType.MARKDOWN, DocumentType.TXT -> {
                findNavController().navigate(R.id.markdownFragment, bundle)
            }
            else -> {
                Toast.makeText(context, "Unsupported file type", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun removeFavorite(favorite: FavoriteItem) {
        prefsRepository.removeFavorite(favorite.uri)
        loadFavorites()
        Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        loadFavorites()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
