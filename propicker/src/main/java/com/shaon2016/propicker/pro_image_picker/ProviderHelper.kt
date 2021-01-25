package com.shaon2016.propicker.pro_image_picker

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.shaon2016.propicker.pro_image_picker.model.Picker
import com.shaon2016.propicker.util.FileUriUtils
import com.shaon2016.propicker.util.FileUtil
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.*
import kotlin.collections.ArrayList


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

            var file = FileUtil.compressImage(
                activity.baseContext,
                uri,
                mMaxWidth.toFloat(),
                mMaxHeight.toFloat()
            )

            val name = file.name
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                file = File(FileUriUtils.getRealPath(activity.baseContext, uri).toString())
            } else {
                file = File(
                    FileUriUtils.copyFileToInternalStorage(uri, "", activity.baseContext).toString()
                )
            }
            Picker(name, uri, file)
        } else {
            val file = File(FileUriUtils.getRealPath(activity.baseContext, uri).toString())
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
            isToCompress -> {
                val newUri = savedUri
                val image = prepareImage(newUri)
                val images = ArrayList<Picker>()
                images.add(image)
                /*This may not be an issue
                In case of Camera with Compress delete(savedUri) not deletes the file
                If you'll try to delete,you wont imageview
                Gallery will have 2 images,but you'll get the uri of compressed one
               */
                setResultAndFinish(images)
            }
            //Camera
            else -> {
                val image = prepareImage(savedUri)
                val images = ArrayList<Picker>()
                images.add(image)
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

    private fun delete(uri: Uri) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val file = File(uri.path.toString())
            file.delete()
            MediaScannerConnection.scanFile(
                activity.baseContext, arrayOf(file.toString()),
                arrayOf(file.name), null
            )
        } else {
            /*
           In Android 10,its not a proper way of deleting image
           */
            val contentResolver = activity.contentResolver
            contentResolver.delete(uri, null, null)
        }

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
            //Getting the cropped image,prepare the view
            val resultUri = UCrop.getOutput(data)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                //Saves to mediastore
                var bitmap: Bitmap? = null
                try {
                    val options = BitmapFactory.Options()
                    options.inSampleSize = 2
                    val inputStream = activity.contentResolver.openInputStream(resultUri!!)
                    bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                val newuri = FileUtil.saveImageGetUri(activity.baseContext, resultUri!!, bitmap!!)
                val image = prepareImage(newuri)
                val images = ArrayList<Picker>()
                images.add(image)
                setResultAndFinish(images)
            } else {
                //clicked image in app package
                val image = prepareImage(resultUri!!)
                val images = ArrayList<Picker>()
                images.add(image)
                setResultAndFinish(images)
            }


        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            Log.e("CropError", "Cropping failed: " + cropError)
            setResultAndFinish(null)
        }
    }
}