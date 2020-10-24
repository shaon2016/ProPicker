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
                        val imageFiles = ProImagePicker.getImages( data)

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

                        val imageFiles = ProImagePicker.getImagesAsFile(this, data)
                        if (imageFiles.size > 0) {
                            Glide.with(this)
                                .load(imageFiles[0])
                                .into(iv)
                        }


//                        val images = ProImagePicker.getImages( data)
//
//                        if (images.size > 0) {
//                            iv.setImageURI(images[0].contentUri)
//                        }

//                        Log.d("DATATAG", images[0].contentUri.toString())


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

                        // As file
//                        ProImagePicker.getCapturedImageFile(intent)

                        iv.setImageURI(imageUri)

                    }
                }
        }
        btnShowCameraOnlyWithoutCrop.setOnClickListener {
            ProImagePicker.with(this)
                .cameraOnly()
                .start { resultCode, data ->
                    if (resultCode == RESULT_OK && data != null) {
                        val file = ProImagePicker.getCapturedImageFile(data)

                        // As file
//                        ProImagePicker.getCapturedImageFile(intent)

                        iv.setImageURI(Uri.fromFile(file))

                    }
                }
        }

    }


}