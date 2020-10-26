package com.shaon2016.propicker.pro_image_picker.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

/**
 * Simple data class to hold information about an image included in the device's MediaStore.
 */
data class MediaStoreImage(
    private val id: Long = 0,
    var displayName: String,
    private val dateAdded: String = "",
    var contentUri: Uri

) : Parcelable {

    var isSelected: Boolean = false

    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readParcelable(Uri::class.java.classLoader) ?: Uri.EMPTY
    ) {
        isSelected = parcel.readByte() != 0.toByte()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(displayName)
        parcel.writeString(dateAdded)
        parcel.writeParcelable(contentUri, flags)
        parcel.writeByte(if (isSelected) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MediaStoreImage> {
        override fun createFromParcel(parcel: Parcel): MediaStoreImage {
            return MediaStoreImage(parcel)
        }

        override fun newArray(size: Int): Array<MediaStoreImage?> {
            return arrayOfNulls(size)
        }
    }

}
