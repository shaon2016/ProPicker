package com.shaon2016.propicker.pro_image_picker

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.shaon2016.propicker.pro_image_picker.model.Image
import com.shaon2016.propicker.util.FileUriUtils
import com.shaon2016.propicker.util.FileUtil
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ProviderHelper(private val activity: AppCompatActivity) {

    /**
     * How many image user can pick
     * */
    private val imageSelectionLength: Int
    private val isCropEnabled: Boolean
    private val isToCompress: Boolean

    // Ucrop & compress
    private val mMaxWidth: Int
    private val mMaxHeight: Int
    private val mCropAspectX: Float
    private val mCropAspectY: Float

    init {
        val bundle = activity.intent.extras!!

        imageSelectionLength = bundle.getInt(ProImagePicker.EXTRA_MULTI_SELECTION, 1)

        // Cropping
        isCropEnabled = bundle.getBoolean(ProImagePicker.EXTRA_CROP, false)
        isToCompress = bundle.getBoolean(ProImagePicker.EXTRA_IS_TO_COMPRESS, false)

        // Get Max Width/Height parameter from Intent
        mMaxWidth = bundle.getInt(ProImagePicker.EXTRA_MAX_WIDTH, 0)
        mMaxHeight = bundle.getInt(ProImagePicker.EXTRA_MAX_HEIGHT, 0)

        // Get Crop Aspect Ratio parameter from Intent
        mCropAspectX = bundle.getFloat(ProImagePicker.EXTRA_CROP_X, 0f)
        mCropAspectY = bundle.getFloat(ProImagePicker.EXTRA_CROP_Y, 0f)

    }

    fun getImageSelectionLength() = imageSelectionLength

    private fun setResultAndFinish(images: ArrayList<Image>?) {
        val i = Intent().apply {
            putParcelableArrayListExtra(ProImagePicker.EXTRA_SELECTED_IMAGES, images)
        }
        activity.setResult(Activity.RESULT_OK, i)
        activity.finish()
    }

    private suspend fun prepareImage(uri: Uri) = withContext(Dispatchers.IO) {
        return@withContext if (isToCompress) {
            val file = FileUtil.compressImage(
                activity.baseContext,
                uri,
                mMaxWidth.toFloat(),
                mMaxHeight.toFloat()
            )
            val name = file.name
            Image(name, uri, file)
        } else {
            val file = File(FileUriUtils.getRealPath(activity.baseContext, uri) ?: "")
            val name = file.name
            Image(name, uri, file)
        }

    }

    suspend fun performGalleryOperationForSingleSelection(uri: Uri) {
        val image = prepareImage(uri)
        val images = ArrayList<Image>()
        images.add(image)
        setResultAndFinish(images)
    }

    suspend fun performGalleryOperationForMultipleSelection(uris: List<Uri>) {
        val images = ArrayList<Image>()

        uris.forEach { uri ->
            val image = prepareImage(uri)
            images.add(image)
        }
        setResultAndFinish(images)
    }

    suspend fun performCameraOperation(savedUri: Uri) {

        when {
            isCropEnabled -> {
                val croppedFile = FileUtil.getImageOutputDirectory(activity.baseContext)
                startCrop(savedUri, croppedFile.toUri())
            }
            else -> {
                val image = prepareImage(savedUri)
                val images = ArrayList<Image>()
                images.add(image)

                // if compress is true then delete the saved image
                if (isToCompress) delete(savedUri)

                setResultAndFinish(images)
            }
        }
    }

    private fun startCrop(sourceUri: Uri, croppedUri: Uri) {
        val uCrop = UCrop.of(sourceUri, croppedUri)

        if (mCropAspectX > 0 && mCropAspectY > 0) {
            uCrop.withAspectRatio(mCropAspectX, mCropAspectY)
        }

        if (mMaxWidth > 0 && mMaxHeight > 0) {
            uCrop.withMaxResultSize(mMaxWidth, mMaxHeight)
        }

        uCrop.start(activity, UCrop.REQUEST_CROP)
    }

    suspend fun delete(uri: Uri) {
        FileUtil.delete(uri.toFile())
    }

    suspend fun handleUCropResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        captureImageUri: Uri?
    ) {

        if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP && data != null) {
            // Deleting Captured image
            captureImageUri?.let {
                delete(it)
            }
            // Getting the cropped image
            val resultUri = UCrop.getOutput(data)

            val image = prepareImage(resultUri!!)
            val images = ArrayList<Image>()
            images.add(image)
            setResultAndFinish(images)

        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            setResultAndFinish(null)
        }
    }
}