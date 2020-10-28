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
                        val list = ProPicker.getSelectedPickerDatas(data)

                        if (list.size > 0) {
                            iv.setImageURI(list[0].uri)
                        }
                    }
                }
        }

        btnGallery.setOnClickListener {
            ProPicker.with(this)
                .galleryOnly()
                .compressImage()
                .multiSelection()
                .start { resultCode, data ->
                    if (resultCode == RESULT_OK && data != null) {
                        val list = ProPicker.getSelectedPickerDatas(data)
                        if (list.size > 0) {
                            Glide.with(this)
                                .load(list[0].file)
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
                        val picker = ProPicker.getPickerData(data)

                        iv.setImageURI(picker?.uri)

                    }
                }
        }
        btnShowCameraOnlyCompress.setOnClickListener {
            ProPicker.with(this)
                .cameraOnly()
                .compressImage()
                .start { resultCode, data ->
                    if (resultCode == RESULT_OK && data != null) {

                        iv.setImageURI(Uri.fromFile(ProPicker.getPickerData(data)?.file))

                    }
                }
        }

    }


}