/*
 * Copyright (c) 2020.
 * @author Md Ashiqul Islam
 * @since 2020/10/22
 */

package com.shaon2016.propicker.pro_image_picker.ui.fragments.gallery

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.shaon2016.propicker.R
import com.shaon2016.propicker.databinding.DialogImageZoomViewBinding
import com.shaon2016.propicker.pro_image_picker.model.MediaStoreImage
import com.shaon2016.propicker.pro_image_picker.ui.ProImagePickerVM
import kotlinx.android.synthetic.main.rv_images_row.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class GalleryImageProviderRvAdapter(
    private val images: MutableList<MediaStoreImage>,
    private val vm: ProImagePickerVM
) : RecyclerView.Adapter<GalleryImageProviderRvAdapter.MyGalleryImageProviderVH>() {

    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyGalleryImageProviderVH {
        context = parent.context
        return MyGalleryImageProviderVH(
            LayoutInflater.from(context).inflate(R.layout.rv_images_row, parent, false)
        )
    }

    override fun onBindViewHolder(holder: MyGalleryImageProviderVH, position: Int) {

        holder.bind(images[holder.adapterPosition])
    }

    override fun getItemCount() = images.size

    inner class MyGalleryImageProviderVH(private val v: View) : RecyclerView.ViewHolder(v) {

        fun bind(mediaStoreImage: MediaStoreImage) {
            Glide.with(context)
                .load(mediaStoreImage.contentUri)
                .thumbnail(0.33f)
                .centerCrop()
                .into(v.iv)

            v.ivCheck.setOnClickListener {
                imageSelectionFunctionality(mediaStoreImage)
            }

            v.iv.setOnClickListener {
                showImageInZoom(mediaStoreImage)
            }

            if (mediaStoreImage.isSelected) {
                v.ivCheck.setImageResource(R.drawable.circle_green_background)
            } else {
                v.ivCheck.setImageResource(R.drawable.circle_black_background_white_2dp_border)
            }
        }

        private fun imageSelectionFunctionality(mediaStoreImage: MediaStoreImage) {
            val selectedImages = vm.selectedImages.value ?: mutableListOf()

            if (vm.imageSelectionLength == 1) {
                // Single selection

                if (selectedImages.contains(mediaStoreImage)) {
                        selectedImages.remove(mediaStoreImage)
                } else {
                    selectedImages.clear()
                    selectedImages.add(mediaStoreImage)
                }

                vm.selectedImages.value = selectedImages


                // Removing check from the previous image
                vm.viewModelScope.launch(Dispatchers.Default) {
                    (0 until images.size).forEach {
                        if (it != adapterPosition && images[it].isSelected) {
                            images[it].isSelected = false

                            vm.viewModelScope.launch {
                                notifyItemChanged(it)
                            }
                        }
                    }
                }

                mediaStoreImage.isSelected = !mediaStoreImage.isSelected
                notifyItemChanged(adapterPosition)


            } else {

                if (selectedImages.contains(mediaStoreImage) || vm.imageSelectionLength > selectedImages.size) {
                    // multi selection
                    mediaStoreImage.isSelected = !mediaStoreImage.isSelected
                    notifyItemChanged(adapterPosition)

                    if (selectedImages.contains(mediaStoreImage))
                        selectedImages.remove(mediaStoreImage)
                    else
                        selectedImages.add(mediaStoreImage)
                }

                vm.selectedImages.value = selectedImages
            }
        }

        private fun showImageInZoom(mediaStoreImage: MediaStoreImage) {
            val binding =
                DialogImageZoomViewBinding.inflate(LayoutInflater.from(context), null, false)

            val d = Dialog(context)
            d.setContentView(binding.root)

            d.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            //binding.iv.setImageURI(mediaStoreImage.contentUri)
            binding.iv.setImageURI(mediaStoreImage.contentUri)
            binding.ivClose.setOnClickListener { d.dismiss() }

            d.show()
        }

    }

}