/*
 * Copyright (c) 2020.
 * @author Md Ashiqul Islam
 * @since 2020/10/22
 */

package com.shaon2016.propicker.pro_image_picker.ui


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.shaon2016.propicker.R
import com.shaon2016.propicker.pro_image_picker.ProImagePicker
import com.shaon2016.propicker.pro_image_picker.model.ImageProvider
import com.shaon2016.propicker.pro_image_picker.ui.fragments.ImageProviderFragment
import com.shaon2016.propicker.pro_image_picker.ui.fragments.gallery.GalleryImageProviderFragment


/** The request code for requesting [Manifest.permission.READ_EXTERNAL_STORAGE] permission. */
private const val PERMISSIONS_REQUEST = 0x1045

internal class ProImagePickerActivity : AppCompatActivity() {

    private val vm: ProImagePickerVM by viewModels()
    private lateinit var provider: ImageProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pro_image_picker)

        title = "Images"

        provider =
            intent?.extras?.getSerializable(ProImagePicker.EXTRA_IMAGE_PROVIDER) as ImageProvider
        vm.imageSelectionLength = intent?.extras?.getInt(ProImagePicker.EXTRA_MULTI_SELECTION) ?: 1

        val bundle = intent.extras!!

        // Cropping
        vm.isCropEnabled = bundle.getBoolean(ProImagePicker.EXTRA_CROP, false)
        vm.isToCompress = bundle.getBoolean(ProImagePicker.EXTRA_IS_TO_COMPRESS, false)

        // Get Max Width/Height parameter from Intent
        vm. mMaxWidth = bundle.getInt(ProImagePicker.EXTRA_MAX_WIDTH, 0)
        vm.mMaxHeight = bundle.getInt(ProImagePicker.EXTRA_MAX_HEIGHT, 0)

        // Get Crop Aspect Ratio parameter from Intent
        vm. mCropAspectX = bundle.getFloat(ProImagePicker.EXTRA_CROP_X, 0f)
        vm. mCropAspectY = bundle.getFloat(ProImagePicker.EXTRA_CROP_Y, 0f)


        loadProvider(provider)


    }

    private fun loadProvider(provider: ImageProvider) {
        when (provider) {
            ImageProvider.GALLERY -> {
                if (havePermission()) {
                    replaceFragment(GalleryImageProviderFragment.newInstance())
                } else {
                    requestPermissions()
                }

            }
            ImageProvider.CAMERA -> {
                if (havePermission()) {

                    replaceFragment(ImageProviderFragment.newInstance())
                } else {
                    requestPermissions()
                }
            }
            else -> {
                finish()
            }
        }
    }


    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }

    // Permission Sections

    private fun havePermission() = (ContextCompat.checkSelfPermission(
        this, Manifest.permission.READ_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(
        this, Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED)

    /**
     * Convenience method to request [Manifest.permission.READ_EXTERNAL_STORAGE] permission.
     */
    private fun requestPermissions() {
        if (!havePermission()) {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            )
            ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST)
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSIONS_REQUEST -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && havePermission()) {
                    loadProvider(provider)
                } else {
                    // If we weren't granted the permission, check to see if we should show
                    // rationale for the permission.
                    showDialogToAcceptPermissions()
                }
                return
            }
        }

    }

    private val startSettingsForResult =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (havePermission()) {
                replaceFragment(GalleryImageProviderFragment.newInstance())
            } else finish()
        }

    private fun goToSettings() {
        val intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        startSettingsForResult.launch(intent)
    }

    private fun showDialogToAcceptPermissions() {
        showPermissionRationalDialog("You need to allow access to view and capture image")
    }

    private fun showPermissionRationalDialog(msg: String) {
        AlertDialog.Builder(this)
            .setMessage(msg)
            .setPositiveButton(
                "OK"
            ) { dialog, which ->
                goToSettings()
            }
            .setNegativeButton("Cancel") { dialog, which ->
                onBackPressed()
            }
            .create()
            .show()


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        for (fragment in supportFragmentManager.fragments) {
            fragment.onActivityResult(requestCode, resultCode, data)
        }
    }
}