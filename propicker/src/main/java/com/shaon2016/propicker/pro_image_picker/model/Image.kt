package com.shaon2016.propicker.pro_image_picker.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.io.File
import java.io.Serializable

@Parcelize
data class Image(val name: String, val uri: Uri, val file: File) : Parcelable