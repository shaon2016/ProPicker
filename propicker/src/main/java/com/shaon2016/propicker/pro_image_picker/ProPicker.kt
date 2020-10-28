/*
 * Copyright (c) 2020.
 * @author Md Ashiqul Islam
 * @since 2020/10/21
 */

package com.shaon2016.propicker.pro_image_picker


import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.github.florent37.inlineactivityresult.kotlin.startForResult
import com.shaon2016.propicker.R
import com.shaon2016.propicker.databinding.DialogImagePickerChooserBinding
import com.shaon2016.propicker.pro_image_picker.model.Picker
import com.shaon2016.propicker.pro_image_picker.model.ImageProvider
import com.shaon2016.propicker.pro_image_picker.ui.ProPickerActivity
import com.shaon2016.propicker.util.FileUriUtils
import com.shaon2016.propicker.util.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ProPicker {
    internal const val EXTRA_MIME_TYPES = "extra.mime_types"
    internal const val EXTRA_IMAGE_PROVIDER = "extra.image_provider"
    internal const val EXTRA_MULTI_SELECTION = "extra.multi_selection"
    internal const val EXTRA_SELECTED_IMAGES = "extra.selected_images"
    internal const val EXTRA_IMAGE_MAX_SIZE = "extra.image_max_size"
    internal const val EXTRA_CROP = "extra.crop"
    internal const val EXTRA_CROP_X = "extra.crop_x"
    internal const val EXTRA_CROP_Y = "extra.crop_y"
    internal const val EXTRA_MAX_WIDTH = "extra.max_width"
    internal const val EXTRA_MAX_HEIGHT = "extra.max_height"
    internal const val EXTRA_IS_TO_COMPRESS = "extra._is_to_compress"

    fun with(activity: Activity): Builder {
        return Builder(activity)
    }

    fun with(fragment: Fragment): Builder {
        return Builder(fragment)
    }

    /**
     * Get all the selected images
     * @param intent
     * */
    fun getSelectedPickerDatas(intent: Intent) =
        intent.getParcelableArrayListExtra<Picker>(EXTRA_SELECTED_IMAGES) ?: ArrayList()

    /**
     * Get all the selected images
     * @param intent
     * */
    fun getPickerData(intent: Intent): Picker? {
        val images = getSelectedPickerDatas(intent)
        return if (images.isNotEmpty()) images[0] else null
    }

    /**
     * Get selected images as Byte Array
     * */
    fun getPickerDataAsByteArray(context: Context, intent: Intent): ArrayList<ByteArray> {
        val arrays = ArrayList<ByteArray>()

        getSelectedPickerDatas(intent).forEach {
            val byteArray = context.contentResolver.openInputStream(it.uri)?.readBytes()
            byteArray?.let {
                arrays.add(byteArray)
            }
        }

        return arrays
    }

    /**
     * It copy file to a directory
     * */
//    fun getImagesAsFile(context: Context, intent: Intent): ArrayList<File> {
//        val files = ArrayList<File>()
//
//        (context as AppCompatActivity).lifecycleScope.launch {
//            getImages(intent).forEach {
//                files.add(getFile(context, it.contentUri))
//            }
//        }
//
//        return files
//    }


    private suspend fun getFile(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        FileUtil.fileFromContentUri(context, uri)
    }


    class Builder(private val activity: Activity) {

        private var fragment: Fragment? = null

        private var imageProvider = ImageProvider.BOTH

        // Mime types restrictions for gallery. by default all mime types are valid
        private var mimeTypes: Array<String> = arrayOf("image/png", "image/jpeg", "image/jpg")

        /*
        * Crop Parameters
        */
        private var cropX: Float = 0f
        private var cropY: Float = 0f
        private var crop: Boolean = false

        // Compress
        private var isToCompress: Boolean = false


        /*
        * Resize Parameters
        */
        private var maxWidth: Int = 0
        private var maxHeight: Int = 0

        // Image selection length
        private var isMultiSelection = false

        /**
         * Max File Size
         */
        private var maxSize: Long = 0

        constructor(fragment: Fragment) : this(fragment.requireActivity()) {
            this.fragment = fragment
        }

        /**
         * Only Capture image using Camera.
         */
        fun cameraOnly(): Builder {
            this.imageProvider = ImageProvider.CAMERA
            return this
        }

        /**
         * Only Pick image from gallery.
         */
        fun galleryOnly(): Builder {
            this.imageProvider = ImageProvider.GALLERY
            return this
        }

        /**
         * Only pick one image
         * */
        fun singleSelection(): Builder {
            isMultiSelection = false
            return this
        }

        /**
         * Pick many image. By default user can pick 5 images
         * */
        fun multiSelection(): Builder {
            isMultiSelection = true
            return this
        }

        /**
         * Set an aspect ratio for crop bounds.
         * User won't see the menu with other ratios options.
         *
         * @param x aspect ratio X
         * @param y aspect ratio Y
         */
        fun crop(x: Float, y: Float): Builder {
            cropX = x
            cropY = y
            return crop()
        }

        /**
         * Crop an image and let user set the aspect ratio.
         */
        fun crop(): Builder {
            this.crop = true
            return this
        }

        /**
         * Crop Square Image, Useful for Profile Image.
         *
         */
        fun cropSquare(): Builder {
            return crop(1f, 1f)
        }

        /**
         * Max Width and Height of final image
         */
        fun maxResultSize(width: Int, height: Int): Builder {
            this.maxWidth = width
            this.maxHeight = height
            return this
        }

        /**
         * @param maxWidth must be greater than 10
         * @param maxHeight must be greater than 10
         * */
        fun compress(maxWidth: Int = 612, maxHeight: Int = 816): Builder {
            if (maxHeight > 10 && maxWidth > 10) {
                this.maxWidth = maxWidth
                this.maxHeight = maxHeight
            }

            isToCompress = true
            return this
        }


        // TODO will implement later
        /**
         * Restrict mime types during gallery fetching, for instance if you do not want GIF images,
         * you can use arrayOf("image/png","image/jpeg","image/jpg")
         * by default array is empty, which indicates no additional restrictions, just images
         * @param mimeTypes
         */
        fun galleryMimeTypes(mimeTypes: Array<String>): Builder {
            this.mimeTypes = mimeTypes
            return this
        }

        /**
         * Select image from gallery
         * */
        fun onlyImage(): Builder {
            this.mimeTypes = arrayOf("image/*")
            return this
        }

        /**
         * Select video from gallery
         * */
        fun onlyVideo(): Builder {
            this.mimeTypes = arrayOf("video/*")
            return this
        }


        /**
         * Start Image Picker Activity
         */
        fun start(completionHandler: ((resultCode: Int, data: Intent?) -> Unit)? = null) {
            if (imageProvider == ImageProvider.BOTH) {
                // Pick Image Provider if not specified
                showImageProviderDialog(completionHandler)
            } else {
                startActivity(completionHandler)
            }
        }

        private fun showImageProviderDialog(completionHandler: ((resultCode: Int, data: Intent?) -> Unit)?) {
            val v = DialogImagePickerChooserBinding.inflate(
                LayoutInflater.from(activity), null, false
            )

            val d = Dialog(activity, R.style.Theme_AppCompat_Dialog_Alert)
            d.setContentView(v.root)

            v.btnCamera.setOnClickListener {
                imageProvider = ImageProvider.CAMERA
                start(completionHandler)
                d.dismiss()
            }

            v.btnGallery.setOnClickListener {
                imageProvider = ImageProvider.GALLERY
                start(completionHandler)
                d.dismiss()
            }

            d.show()

        }

        /**
         * Start ImagePickerActivity with given Argument
         */
        private fun startActivity(completionHandler: ((resultCode: Int, data: Intent?) -> Unit)? = null) {
            val intent = Intent(activity, ProPickerActivity::class.java)
            intent.putExtras(getBundle())
            if (fragment != null) {

                fragment?.startForResult(intent) { result ->
                    completionHandler?.invoke(result.resultCode, result.data)
                }?.onFailed { result ->
                    completionHandler?.invoke(result.resultCode, result.data)
                }
            } else {
                (activity as AppCompatActivity).startForResult(intent) { result ->
                    completionHandler?.invoke(result.resultCode, result.data)
                }.onFailed { result ->
                    completionHandler?.invoke(result.resultCode, result.data)
                }
            }
        }

        /**
         * Get Bundle for ProImagePickerActivity
         */
        private fun getBundle(): Bundle {
            return Bundle().apply {
                putSerializable(EXTRA_IMAGE_PROVIDER, imageProvider)
                putStringArray(EXTRA_MIME_TYPES, mimeTypes)
                putBoolean(EXTRA_MULTI_SELECTION, isMultiSelection)

                putBoolean(EXTRA_CROP, crop)
                putBoolean(EXTRA_IS_TO_COMPRESS, isToCompress)

                putLong(EXTRA_IMAGE_MAX_SIZE, maxSize)
                putFloat(EXTRA_CROP_X, cropX)
                putFloat(EXTRA_CROP_Y, cropY)
                putInt(EXTRA_MAX_WIDTH, maxWidth)
                putInt(EXTRA_MAX_HEIGHT, maxHeight)

            }
        }

    }

}