package com.shaon2016.propicker.pro_image_picker.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.File

@Parcelize
data class Picker(val name: String, val uri: Uri, val file: File) : Parcelable