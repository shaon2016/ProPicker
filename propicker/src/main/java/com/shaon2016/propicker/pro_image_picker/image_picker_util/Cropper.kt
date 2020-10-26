/*
 * Copyright (c) 2020.
 * @author Md Ashiqul Islam
 * @since 2020/10/23
 */

package com.shaon2016.propicker.pro_image_picker.image_picker_util

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import androidx.lifecycle.lifecycleScope
import com.shaon2016.propicker.pro_image_picker.ProImagePicker
import com.shaon2016.propicker.util.FileUtil
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class Cropper(private val activity: AppCompatActivity) {
    private val mCrop: Boolean
    private val isToCompress: Boolean

    // Ucrop
    private val mMaxWidth: Int
    private val mMaxHeight: Int
    private val mCropAspectX: Float
    private val mCropAspectY: Float

    companion object {
        internal val CROPPED_FILE = "extras.crop_file"
        internal val SOURCE_FILE = "extras.source_file"
    }

    init {
        val bundle = activity.intent.extras!!

        // Cropping
        mCrop = bundle.getBoolean(ProImagePicker.EXTRA_CROP, false)
        isToCompress = bundle.getBoolean(ProImagePicker.EXTRA_IS_TO_COMPRESS, false)

        // Get Max Width/Height parameter from Intent
        mMaxWidth = bundle.getInt(ProImagePicker.EXTRA_MAX_WIDTH, 0)
        mMaxHeight = bundle.getInt(ProImagePicker.EXTRA_MAX_HEIGHT, 0)

        // Get Crop Aspect Ratio parameter from Intent
        mCropAspectX = bundle.getFloat(ProImagePicker.EXTRA_CROP_X, 0f)
        mCropAspectY = bundle.getFloat(ProImagePicker.EXTRA_CROP_Y, 0f)


//        // Get File Directory
//        val fileDir = bundle.getString(ImagePicker.EXTRA_SAVE_DIRECTORY)
//        fileDir?.let {
//            mFileDir = File(it)
//        }
    }


    /**
     * Check if crop should be enabled or not
     *
     * @return Boolean. True if Crop should be enabled else false.
     */
    fun isCropEnabled() = mCrop

    fun isToCompress() = isToCompress

    /**
     * Start Crop
     */
    fun startCropUsingUCrop(sourceFile: File, croppedFile: File) {
        cropImage(sourceFile, croppedFile)
    }

    private fun cropImage(sourceFile: File, croppedFile: File) {
        val uCrop = UCrop.of(Uri.fromFile(sourceFile), Uri.fromFile(croppedFile))

        if (mCropAspectX > 0 && mCropAspectY > 0) {
            uCrop.withAspectRatio(mCropAspectX, mCropAspectY)
        }

        if (mMaxWidth > 0 && mMaxHeight > 0) {
            uCrop.withMaxResultSize(mMaxWidth, mMaxHeight)
        }

        uCrop.start(activity, UCrop.REQUEST_CROP)

    }

    suspend fun compress(uri: Uri) = FileUtil.compressImage(
        activity.baseContext,
        uri,
        mMaxWidth.toFloat(),
        mMaxHeight.toFloat()
    )

    suspend fun delete(uri: Uri) {
        FileUtil.delete(uri.toFile())
    }


}