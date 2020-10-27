/*
 * Copyright (c) 2020.
 * @author Md Ashiqul Islam
 * @since 2020/10/22
 */

package com.shaon2016.propicker.pro_image_picker.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.net.toFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.shaon2016.propicker.pro_image_picker.ProImagePicker
import com.shaon2016.propicker.pro_image_picker.model.MediaStoreImage
import com.shaon2016.propicker.util.FileUtil
import com.shaon2016.propicker.util.formattedDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

internal class ProImagePickerVM(application: Application) : AndroidViewModel(application) {
    /**
     * It holds the selected image and update the toolbar title
     * */
    var selectedImages = MutableLiveData<MutableList<MediaStoreImage>>()


    /**
     * It loads all the image for gallery
     * */
    private val _images = MutableLiveData<List<MediaStoreImage>>()
    val images: LiveData<List<MediaStoreImage>> get() = _images

    private var contentObserver: ContentObserver? = null




    /**
     * Performs a one shot load of images from [MediaStore.Images.Media.EXTERNAL_CONTENT_URI] into
     * the [_images] [LiveData] above.
     */
    fun loadImages() {
        viewModelScope.launch {
            val imageList = queryImages()
            _images.postValue(imageList)

            if (contentObserver == null) {
                contentObserver = getApplication<Application>().contentResolver.registerObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                ) {
                    loadImages()
                }
            }
        }
    }


    private suspend fun queryImages(): List<MediaStoreImage> {
        val images = mutableListOf<MediaStoreImage>()


        withContext(Dispatchers.IO) {

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED
            )

            val selection = "${MediaStore.Images.Media.DATE_ADDED} >= ?"

            val selectionArgs = arrayOf(
                // Release day of the G1. :)
                dateToTimestamp(day = 22, month = 10, year = 2008).toString()
            )

            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            getApplication<Application>().contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->

                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateModifiedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val displayNameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)

                while (cursor.moveToNext()) {

                    // Here we'll use the column indexs that we found above.
                    val id = cursor.getLong(idColumn)
                    val dateModified =
                        Date(TimeUnit.SECONDS.toMillis(cursor.getLong(dateModifiedColumn)))
                    val displayName = cursor.getString(displayNameColumn)

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val image =
                        MediaStoreImage(id, displayName, dateModified.formattedDate(), contentUri)
                    images += image

                }
            }
        }

        return images
    }


    /**
     * Convenience method to convert a day/month/year date into a UNIX timestamp.
     *
     * We're suppressing the lint warning because we're not actually using the date formatter
     * to format the date to display, just to specify a format to use to parse it, and so the
     * locale warning doesn't apply.
     */
    @Suppress("SameParameterValue")
    @SuppressLint("SimpleDateFormat")
    private fun dateToTimestamp(day: Int, month: Int, year: Int): Long =
        SimpleDateFormat("dd.MM.yyyy").let { formatter ->
            TimeUnit.MICROSECONDS.toSeconds(formatter.parse("$day.$month.$year")?.time ?: 0)
        }



    /**
     * Since we register a [ContentObserver], we want to unregister this when the `ViewModel`
     * is being released.
     */
    override fun onCleared() {
        contentObserver?.let {
            getApplication<Application>().contentResolver.unregisterContentObserver(it)
        }
    }

}

/**
 * Convenience extension method to register a [ContentObserver] given a lambda.
 */
private fun ContentResolver.registerObserver(
    uri: Uri,
    observer: (selfChange: Boolean) -> Unit
): ContentObserver {
    val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            observer(selfChange)
        }
    }
    registerContentObserver(uri, true, contentObserver)
    return contentObserver
}

private const val TAG = "ProImagePickerVM"