package com.shaon2016.propickersample

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import com.shaon2016.propicker.pro_image_picker.ProPicker
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        btnChooser.setOnClickListener {
            ProPicker.with(this)
                .start { resultCode, data ->
                    if (resultCode == RESULT_OK && data != null) {
                        val imageFiles = ProPicker.getSelectedPickerDatas(data)

                        if (imageFiles.size > 0) {
                            iv.setImageURI(imageFiles[0].uri)
                        }
                    }
                }
        }

        btnGallery.setOnClickListener {
            ProPicker.with(this)
                .galleryOnly()
                .multiSelection()
                .onlyImage()
                .start { resultCode, data ->
                    if (resultCode == RESULT_OK && data != null) {

                        val images = ProPicker.getSelectedPickerDatas(data)
                        if (images.size > 0) {
                            Glide.with(this)
                                .load(images[0].file)
                                .into(iv)
                        }

                    }
                }
        }

        btnShowCameraOnlyWithCrop.setOnClickListener {
            ProPicker.with(this)
                .cameraOnly()
                .crop()
                .start { resultCode, data ->
                    if (resultCode == RESULT_OK && data != null) {
                        val image = ProPicker.getPickerData(data)

                        iv.setImageURI(image?.uri)

                    }
                }
        }
        btnShowCameraOnlyCompress.setOnClickListener {
            ProPicker.with(this)
                .cameraOnly()
                .compress()
                .start { resultCode, data ->
                    if (resultCode == RESULT_OK && data != null) {

                        iv.setImageURI(Uri.fromFile(ProPicker.getPickerData(data)?.file))

                    }
                }
        }

    }


}