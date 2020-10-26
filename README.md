# ProPicker

A simple library to select images from the gallery and camera. 

There are many libraries out there. May be some serves your purposes but not satisfactory. This library is different from the others.

Why should you use it? 

* CameraX library to capture images. 
* It also uses UCrop library to crop images. 
* It uses best compression to compress your image without loosing image's quality.

Step 1. Add the JitPack repository to your build file

```
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
```

Step 2. Add the dependency

```
dependencies {
    implementation 'com.github.shaon2016:ProPicker:0.1.3'
}

```

# To working with this library you have to do the below work.......
 
Add this permissions in your androidManifest.xml file

```
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

    <uses-feature android:name="android.hardware.camera.any" />

    <uses-permission android:name="android.permission.CAMERA" />
    
```

Add this in your build.gradle app module

```
android {

    //.........
    
    kotlinOptions {
        jvmTarget = "1.8"
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }

    buildFeatures {
        dataBinding true
    }

}
```

# Screenshot


![](screenshot/image1.jpeg)     ![](screenshot/image2.jpeg) 

## Start Pro image picker activity

The simplest way to start 

```
            ProImagePicker.with(this)
                .start { resultCode, data ->
                    if (resultCode == RESULT_OK && data != null) {
                        val imageFiles = ProImagePicker.getImages(data)

                        if (imageFiles.size > 0) {
                            iv.setImageURI(imageFiles[0].contentUri)
                        }
                    }
                }
```

What you can do with ImagePicker

Camera

```
            ProImagePicker.with(this)
                .cameraOnly()
                .crop()
                .start { resultCode, data ->
                    if (resultCode == RESULT_OK && data != null) {
                        val imageUri = ProImagePicker.getCapturedImageUri(data)

                        iv.setImageURI(imageUri)

                    }
                }
```

Gallery

```
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
                    }
                }
```

##### Function that offers this library

1. cameraOnly() -> To open the CameraX only
2. galleryOnly() -> To open the gallery view only
3. crop() -> Only works with camera
4. ProImagePicker.getImagesAsFile(this, intent) -> Returns all the images as File (Should not use in Android 10 or above)
5. ProImagePicker.getImagesAsByteArray(this, intent) -> Returns all the images as ByteArray (You should always use it. Using this function you can load image in imageview using Glide and you can upload images or videos to server using Retrofit library.
6. ProImagePicker.getCapturedImageFile(intent)
7. ProImagePicker.getCapturedImageUri(intent)
8. ProImagePicker.getImages(intent: Intent) -> Get all the images 
