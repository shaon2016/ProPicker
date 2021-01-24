package com.shaon2016.propickersample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.shaon2016.propicker.pro_image_picker.ProPicker
import com.shaon2016.propickersample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)


        binding.btnChooser.setOnClickListener {
            ProPicker.with(this)
                .start { resultCode, data ->
                    if (resultCode == RESULT_OK && data != null) {
                        val l = ProPicker.getPickerData(data)
                        binding.iv.setImageURI(l?.uri)
                    }
                }
        }

        binding.btnGallery.setOnClickListener {
            ProPicker.with(this)
                .galleryOnly()
                .compressImage()
                .start { resultCode, data ->
                    if (resultCode == RESULT_OK && data != null) {
                        val list = ProPicker.getSelectedPickerDatas(data)
                        if (list.size > 0) {
                            Glide.with(this)
                                .load(list[0].file)
                                .into(binding.iv)
                        }

                    }
                }
        }


        binding.btnShowCameraOnlyWithCrop.setOnClickListener {
            ProPicker.with(this)
                .cameraOnly()
                .crop()
                .start { resultCode, data ->
                    if (resultCode == RESULT_OK && data != null) {
                        val picker = ProPicker.getPickerData(data)
                        binding.iv.setImageURI(picker?.uri)

                    }
                }
        }

        binding.btnShowCameraOnlyCompress.setOnClickListener {
            ProPicker.with(this)
                .cameraOnly()
                .compressImage()
                .start { resultCode, data ->
                    if (resultCode == RESULT_OK && data != null) {
                        val picker = ProPicker.getPickerData(data)
                        binding.iv.setImageURI(picker?.uri)

                    }
                }
        }


    }


}