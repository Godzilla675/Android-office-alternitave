package com.officesuite.app.ui.onboarding

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.officesuite.app.R
import com.officesuite.app.databinding.FragmentOnboardingBinding
import com.officesuite.app.databinding.ItemOnboardingPageBinding

/**
 * Onboarding fragment that shows a first-time user tutorial.
 * Displays a series of slides explaining the app's features.
 */
class OnboardingFragment : Fragment() {
    
    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var onboardingAdapter: OnboardingAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViewPager()
        setupClickListeners()
    }
    
    private fun setupViewPager() {
        val pages = listOf(
            OnboardingPage(
                title = "Welcome to Office Suite",
                description = "Your all-in-one document viewer and scanner. View PDFs, Word documents, PowerPoint presentations, and more.",
                iconRes = R.drawable.ic_document
            ),
            OnboardingPage(
                title = "Scan Documents",
                description = "Use your camera to scan documents with automatic border detection and OCR text recognition.",
                iconRes = R.drawable.ic_scanner
            ),
            OnboardingPage(
                title = "Convert Files",
                description = "Convert between different file formats. PDF to Word, presentations to PDF, and more.",
                iconRes = R.drawable.ic_convert
            ),
            OnboardingPage(
                title = "Get Started",
                description = "You're all set! Tap the button below to start exploring your documents.",
                iconRes = R.drawable.ic_check
            )
        )
        
        onboardingAdapter = OnboardingAdapter(pages)
        binding.viewPager.adapter = onboardingAdapter
        
        // Setup page indicator
        TabLayoutMediator(binding.pageIndicator, binding.viewPager) { _, _ ->
            // Empty - we just need the tabs as indicators
        }.attach()
        
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateButtonsVisibility(position, pages.size)
            }
        })
    }
    
    private fun setupClickListeners() {
        binding.btnSkip.setOnClickListener {
            completeOnboarding()
        }
        
        binding.btnNext.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            val totalPages = onboardingAdapter.itemCount
            
            if (currentItem < totalPages - 1) {
                binding.viewPager.currentItem = currentItem + 1
            } else {
                completeOnboarding()
            }
        }
        
        binding.btnBack.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            if (currentItem > 0) {
                binding.viewPager.currentItem = currentItem - 1
            }
        }
    }
    
    private fun updateButtonsVisibility(position: Int, totalPages: Int) {
        binding.btnBack.visibility = if (position > 0) View.VISIBLE else View.INVISIBLE
        binding.btnSkip.visibility = if (position < totalPages - 1) View.VISIBLE else View.INVISIBLE
        binding.btnNext.text = if (position == totalPages - 1) "Get Started" else "Next"
    }
    
    private fun completeOnboarding() {
        // Mark onboarding as complete
        OnboardingManager.setOnboardingComplete(requireContext())
        
        // Navigate to home
        findNavController().navigate(R.id.action_onboarding_to_home)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * Data class representing an onboarding page.
 */
data class OnboardingPage(
    val title: String,
    val description: String,
    val iconRes: Int
)

/**
 * Adapter for onboarding ViewPager2.
 */
class OnboardingAdapter(
    private val pages: List<OnboardingPage>
) : RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val binding = ItemOnboardingPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OnboardingViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(pages[position])
    }
    
    override fun getItemCount(): Int = pages.size
    
    class OnboardingViewHolder(
        private val binding: ItemOnboardingPageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(page: OnboardingPage) {
            binding.textTitle.text = page.title
            binding.textDescription.text = page.description
            binding.imageIcon.setImageResource(page.iconRes)
        }
    }
}

/**
 * Manager for tracking onboarding completion state.
 */
object OnboardingManager {
    
    private const val PREFS_NAME = "onboarding_prefs"
    private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Checks if onboarding has been completed.
     */
    fun isOnboardingComplete(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ONBOARDING_COMPLETE, false)
    }
    
    /**
     * Marks onboarding as complete.
     */
    fun setOnboardingComplete(context: Context) {
        getPrefs(context).edit().putBoolean(KEY_ONBOARDING_COMPLETE, true).apply()
    }
    
    /**
     * Resets onboarding state (for testing or re-showing tutorial).
     */
    fun resetOnboarding(context: Context) {
        getPrefs(context).edit().putBoolean(KEY_ONBOARDING_COMPLETE, false).apply()
    }
}
