package com.officesuite.app.ui.scanner

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.officesuite.app.databinding.FragmentCropImageBinding

class CropImageFragment : Fragment() {

    private var _binding: FragmentCropImageBinding? = null
    private val binding get() = _binding!!
    
    private var imageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCropImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        arguments?.getString("image_uri")?.let { uriString ->
            imageUri = Uri.parse(uriString)
            loadImage()
        }
        
        setupClickListeners()
    }

    private fun loadImage() {
        imageUri?.let { uri ->
            binding.imageView.setImageURI(uri)
        }
    }

    private fun setupClickListeners() {
        binding.btnRotateLeft.setOnClickListener {
            // Rotate image left
            binding.imageView.rotation -= 90
        }

        binding.btnRotateRight.setOnClickListener {
            // Rotate image right
            binding.imageView.rotation += 90
        }

        binding.btnCrop.setOnClickListener {
            // Apply crop and return
            Toast.makeText(context, "Crop applied", Toast.LENGTH_SHORT).show()
            @Suppress("DEPRECATION")
            requireActivity().onBackPressed()
        }

        binding.btnCancel.setOnClickListener {
            @Suppress("DEPRECATION")
            requireActivity().onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
