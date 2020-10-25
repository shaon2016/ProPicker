package com.shaon2016.propickersample

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import com.shaon2016.propicker.pro_image_picker.ProImagePicker
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        btnChooser.setOnClickListener {
            ProImagePicker.with(this)
                .start { resultCode, data ->
                    if (resultCode == RESULT_OK && data != null) {
                        val imageFiles = ProImagePicker.getImages(data)

                        if (imageFiles.size > 0) {
                            iv.setImageURI(imageFiles[0].contentUri)
                        }
                    }
                }
        }

        btnGallery.setOnClickListener {
            ProImagePicker.with(this)
                .galleryOnly()
                .multiSelection(10)
                .start { resultCode, data ->
                    if (resultCode == RESULT_OK && data != null) {


                        val byteArrays = ProImagePicker.getImagesAsByteArray(this, data)
                        if (byteArrays.size > 0) {
                            Glide.with(this)
                                .load(byteArrays[0])
                                .into(iv)
                        }
                    }
                }
        }
        btnShowCameraOnlyWithCrop.setOnClickListener {
            ProImagePicker.with(this)
                .cameraOnly()
                .crop()
                .start { resultCode, data ->
                    if (resultCode == RESULT_OK && data != null) {
                        val imageUri = ProImagePicker.getCapturedImageUri(data)

                        iv.setImageURI(imageUri)

                    }
                }
        }
        btnShowCameraOnlyWithoutCrop.setOnClickListener {
            ProImagePicker.with(this)
                .cameraOnly()
                .compress()
                .crop()
                .start { resultCode, data ->
                    if (resultCode == RESULT_OK && data != null) {
                        val file = ProImagePicker.getCapturedImageFile(data)

                        iv.setImageURI(Uri.fromFile(file))

                    }
                }
        }

    }


}