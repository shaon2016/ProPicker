/*
 * Copyright (c) 2020.
 * @author Md Ashiqul Islam
 * @since 2020/10/22
 */

package com.shaon2016.propicker.pro_image_picker.ui.fragments.gallery

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.shaon2016.propicker.databinding.FragmentGalleryImageProviderBinding
import com.shaon2016.propicker.pro_image_picker.ProImagePicker
import com.shaon2016.propicker.pro_image_picker.model.MediaStoreImage
import com.shaon2016.propicker.pro_image_picker.ui.ProImagePickerVM
import com.shaon2016.propicker.util.D
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception


internal class GalleryImageProviderFragment : Fragment() {

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

        val d = D.showProgressDialog(requireContext(), "Loading.....")
        vm.loadImages()
        vm.images.observe(viewLifecycleOwner, {
            it?.let {
                binding.rvImages.adapter =
                    GalleryImageProviderRvAdapter(it as MutableList<MediaStoreImage>, vm)
            }
            d.dismiss()
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

                if (vm.isToCompress) {
                    var images = ArrayList<MediaStoreImage>()

                    val d = D.showProgressDialog(requireContext(), "Compressing........")

                    lifecycleScope.launch {
                        images = getCompressedImages(images)

                        d.dismiss()

                        intent.putParcelableArrayListExtra(
                            ProImagePicker.EXTRA_SELECTED_IMAGES,
                            images
                        )
                        requireActivity().setResult(Activity.RESULT_OK, intent)
                        requireActivity().finish()
                    }

                } else {
                    intent.putParcelableArrayListExtra(
                        ProImagePicker.EXTRA_SELECTED_IMAGES,
                        vm.selectedImages.value!! as ArrayList
                    )
                    requireActivity().setResult(Activity.RESULT_OK, intent)
                    requireActivity().finish()
                }


            } else
                requireActivity().finish()
        }

        binding.ivClose.setOnClickListener {
            requireActivity().finish()
        }
    }


    private suspend fun getCompressedImages(images: ArrayList<MediaStoreImage>) =
        withContext(Dispatchers.Default) {
            val length = vm.selectedImages.value!!.size

            try {
                (0 until length).forEach { i ->
                    val mediaStoreImage = vm.selectedImages.value!![i]

                    mediaStoreImage.contentUri = vm.compress(mediaStoreImage.contentUri)
                    mediaStoreImage.displayName = mediaStoreImage.contentUri.toFile().name

                    images.add(mediaStoreImage)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }


            return@withContext images
        }


    companion object {

        @JvmStatic
        fun newInstance() = GalleryImageProviderFragment()
    }
}