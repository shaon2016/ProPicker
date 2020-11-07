package com.shaon2016.propicker.pro_image_picker

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import com.shaon2016.propicker.pro_image_picker.model.Picker
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
    private val isMultiSelection: Boolean
    private val isCropEnabled: Boolean
    private val isToCompress: Boolean

    // Ucrop & compress
    private val mMaxWidth: Int
    private val mMaxHeight: Int
    private val mCropAspectX: Float
    private val mCropAspectY: Float
    private val mGalleryMimeTypes: Array<String>

    init {
        val bundle = activity.intent.extras!!

        isMultiSelection = bundle.getBoolean(ProPicker.EXTRA_MULTI_SELECTION, false)

        // Cropping
        isCropEnabled = bundle.getBoolean(ProPicker.EXTRA_CROP, false)
        isToCompress = bundle.getBoolean(ProPicker.EXTRA_IS_TO_COMPRESS, false)

        // Get Max Width/Height parameter from Intent
        mMaxWidth = bundle.getInt(ProPicker.EXTRA_MAX_WIDTH, 0)
        mMaxHeight = bundle.getInt(ProPicker.EXTRA_MAX_HEIGHT, 0)

        // Get Crop Aspect Ratio parameter from Intent
        mCropAspectX = bundle.getFloat(ProPicker.EXTRA_CROP_X, 0f)
        mCropAspectY = bundle.getFloat(ProPicker.EXTRA_CROP_Y, 0f)

        mGalleryMimeTypes = bundle.getStringArray(ProPicker.EXTRA_MIME_TYPES) as Array<String>


    }

    fun isToCompress() = isToCompress

    fun getGalleryMimeTypes() = mGalleryMimeTypes

    fun getMultiSelection() = isMultiSelection

    fun setResultAndFinish(images: ArrayList<Picker>?) {
        val i = Intent().apply {
            putParcelableArrayListExtra(ProPicker.EXTRA_SELECTED_IMAGES, images)
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
            Picker(name, Uri.fromFile(file), file)
        } else {
            val file = File(FileUriUtils.getRealPath(activity.baseContext, uri) ?: "")
            val name = file.name
            Picker(name, uri, file)
        }
    }

    suspend fun performGalleryOperationForSingleSelection(uri: Uri): ArrayList<Picker> {
        val image = prepareImage(uri)
        val images = ArrayList<Picker>()
        images.add(image)
        // setResultAndFinish(images)
        return images
    }

    suspend fun performGalleryOperationForMultipleSelection(uris: List<Uri>): ArrayList<Picker> {
        val images = ArrayList<Picker>()

        uris.forEach { uri ->
            val image = prepareImage(uri)
            images.add(image)
        }
        //setResultAndFinish(images)
        return images
    }

    suspend fun performCameraOperation(savedUri: Uri) {
        when {
            isCropEnabled -> {
                val croppedFile = FileUtil.getImageOutputDirectory(activity.baseContext)
                startCrop(savedUri, Uri.fromFile(croppedFile))
            }
            else -> {
                val image = prepareImage(savedUri)
                val images = ArrayList<Picker>()
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

    private suspend fun delete(uri: Uri) {
        FileUtil.delete(File(uri.path))
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
            val images = ArrayList<Picker>()
            images.add(image)
            setResultAndFinish(images)

        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            setResultAndFinish(null)
        }
    }
}