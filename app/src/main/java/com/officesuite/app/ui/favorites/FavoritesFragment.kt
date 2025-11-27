package com.officesuite.app.ui.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.officesuite.app.R
import com.officesuite.app.data.repository.FavoriteItem
import com.officesuite.app.data.repository.PreferencesRepository
import com.officesuite.app.databinding.FragmentFavoritesBinding
import com.officesuite.app.utils.NavigationUtils

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
        NavigationUtils.navigateToViewer(this, favorite.uri, favorite.type)
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
