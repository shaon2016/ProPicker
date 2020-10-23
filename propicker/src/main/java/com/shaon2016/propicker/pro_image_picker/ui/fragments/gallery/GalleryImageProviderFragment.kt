/*
 * Copyright (c) 2020.
 * @author Md Ashiqul Islam
 * @since 2020/10/22
 */

package com.shaon2016.propicker.pro_image_picker.ui.fragments.gallery

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.shaon2016.propicker.databinding.FragmentGalleryImageProviderBinding
import com.shaon2016.propicker.pro_image_picker.ProImagePicker
import com.shaon2016.propicker.pro_image_picker.model.MediaStoreImage
import com.shaon2016.propicker.pro_image_picker.ui.ProImagePickerVM


class GalleryImageProviderFragment : Fragment() {

    private val vm by lazy {
        ViewModelProvider(requireActivity()).get(ProImagePickerVM::class.java)
    }

    private lateinit var binding: FragmentGalleryImageProviderBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentGalleryImageProviderBinding.inflate(inflater, container, false)

        setToolbar()
        return binding.root
    }

    private fun setToolbar() {
        val appCompatActivity = requireActivity() as AppCompatActivity
        appCompatActivity.setSupportActionBar(binding.toolbar)
        binding.toolbar.setTitleTextColor(
            ContextCompat.getColor(
                requireContext(),
                android.R.color.white
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.rvImages.layoutManager = GridLayoutManager(requireContext(), 2)

        vm.loadImages()
        vm.images.observe(viewLifecycleOwner, {
            it?.let {
                binding.rvImages.adapter =
                    GalleryImageProviderRvAdapter(it as MutableList<MediaStoreImage>, vm)
            }
        })

        vm.selectedImages.observe(viewLifecycleOwner, {
            it?.let {
                binding.toolbar.title =
                    if (it.size > 0) "Images (${it.size}/${vm.imageSelectionLength})"
                    else "Images"
            }
        })

        binding.fabDone.setOnClickListener {
            if (vm.selectedImages.value != null && vm.selectedImages.value!!.size > 0) {
                val intent = Intent()
                intent.putParcelableArrayListExtra(
                    ProImagePicker.EXTRA_SELECTED_IMAGES,
                    vm.selectedImages.value!! as ArrayList
                )
                requireActivity().setResult(Activity.RESULT_OK, intent)
            }
            requireActivity().finish()
        }

        binding.ivClose.setOnClickListener {
            requireActivity().finish()
        }
    }


    companion object {

        @JvmStatic
        fun newInstance() = GalleryImageProviderFragment()
    }
}