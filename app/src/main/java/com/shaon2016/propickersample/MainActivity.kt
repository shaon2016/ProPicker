package com.shaon2016.propickersample

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.shaon2016.propicker.pro_image_picker.ProPicker

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        findViewById<TextView>(R.id.btnChooser).setOnClickListener {
            ProPicker.with(this)
                .start { resultCode, data ->
                    if (resultCode == RESULT_OK && data != null) {
                        val l = ProPicker.getPickerData(data)

                        findViewById<ImageView>(R.id.iv).setImageURI(l?.uri)
                    }
                }
        }

        findViewById<TextView>(R.id.btnGallery).setOnClickListener {
            ProPicker.with(this)
                .galleryOnly()
                .compressImage()
                .start { resultCode, data ->
                    if (resultCode == RESULT_OK && data != null) {
                        val list = ProPicker.getSelectedPickerDatas(data)
                        if (list.size > 0) {
                            Glide.with(this)
                                .load(list[0].file)
                                .into(findViewById<ImageView>(R.id.iv))
                        }

                    }
                }
        }

        findViewById<TextView>(R.id.btnShowCameraOnlyWithCrop).setOnClickListener {
            ProPicker.with(this)
                .cameraOnly()
                .crop()
                .start { resultCode, data ->
                    if (resultCode == RESULT_OK && data != null) {
                        val picker = ProPicker.getPickerData(data)

                        findViewById<ImageView>(R.id.iv).setImageURI(picker?.uri)

                    }
                }
        }
        findViewById<TextView>(R.id.btnShowCameraOnlyCompress).setOnClickListener {
            ProPicker.with(this)
                .cameraOnly()
                .compressImage()
                .start { resultCode, data ->
                    if (resultCode == RESULT_OK && data != null) {

                        findViewById<ImageView>(R.id.iv).setImageURI(ProPicker.getPickerData(data)?.uri)

                    }
                }
        }

    }


}