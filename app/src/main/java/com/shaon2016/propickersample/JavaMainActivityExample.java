package com.shaon2016.propickersample;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.shaon2016.propicker.pro_image_picker.ProPicker;
import com.shaon2016.propickersample.databinding.ActivityMain2Binding;

import java.util.Objects;

public class JavaMainActivityExample extends AppCompatActivity {
    ActivityMain2Binding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMain2Binding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);


        binding.btnChooser.setOnClickListener(view1 -> ProPicker.with(this).start((integer, intent) -> {

            ImageView iv = findViewById(R.id.iv);
            Uri imageuri = Objects.requireNonNull(ProPicker.getPickerData(intent)).getUri();
            iv.setImageURI(imageuri);
            return null;
        }));


        /*ProPicker.with(this)
                .compressImage()
                .cameraOnly()
                .start((integer, intent) -> {

                    ImageView iv = findViewById(R.id.iv);
                    iv.setImageURI(ProPicker.getPickerData(intent).getUri());
                    return null;
                });*/
    }


}