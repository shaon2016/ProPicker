package com.shaon2016.propickersample

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
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
                            iv.setImageURI(imageFiles[0].uri)
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

                        val images = ProImagePicker.getImages(data)
                        if (images.size > 0) {
                            Glide.with(this)
                                .load(images[0].file)
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
                        val image = ProImagePicker.getImage(data)

                        iv.setImageURI(image?.uri)

                    }
                }
        }
        btnShowCameraOnlyCompress.setOnClickListener {
            ProImagePicker.with(this)
                .cameraOnly()
                .compress()
                .start { resultCode, data ->
                    if (resultCode == RESULT_OK && data != null) {

                        iv.setImageURI(Uri.fromFile(ProImagePicker.getImage(data)?.file))

                    }
                }
        }

    }


}