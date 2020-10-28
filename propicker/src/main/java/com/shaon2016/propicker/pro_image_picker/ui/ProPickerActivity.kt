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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.shaon2016.propicker.R
import com.shaon2016.propicker.pro_image_picker.ProPicker
import com.shaon2016.propicker.pro_image_picker.ProviderHelper
import com.shaon2016.propicker.pro_image_picker.model.ImageProvider
import com.shaon2016.propicker.util.D
import kotlinx.coroutines.launch

/** The request code for requesting [Manifest.permission.READ_EXTERNAL_STORAGE] permission. */
private const val PERMISSIONS_REQUEST = 0x1045

internal class ProPickerActivity : AppCompatActivity() {
    private val providerHelper by lazy { ProviderHelper(this) }
    private lateinit var imageProvider: ImageProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pro_image_picker)

        imageProvider =
            intent?.extras?.getSerializable(ProPicker.EXTRA_IMAGE_PROVIDER) as ImageProvider

        loadProvider(imageProvider)

    }

    private fun loadProvider(provider: ImageProvider) {
        when (provider) {
            ImageProvider.GALLERY -> {
                prepareGallery()
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

    private fun prepareGallery() {
        val d = if (providerHelper.isToCompress())
            D.showProgressDialog(this, "Compressing....", false)
        else D.showProgressDialog(this, "Processing....", false)

        if (!providerHelper.getMultiSelection()) {
            // Single choice
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri == null) finish()
                uri?.let {
                    lifecycleScope.launch {
                        d.show()
                       val images =  providerHelper.performGalleryOperationForSingleSelection(uri)
                        d.dismiss()
                        providerHelper.setResultAndFinish(images)
                    }
                }

            }.launch(providerHelper.getGalleryMimeTypes())
        } else {
            // Multiple choice
            registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
                if (uris == null) finish()
                uris?.let {
                    lifecycleScope.launch {
                        d.show()
                        val images  = providerHelper.performGalleryOperationForMultipleSelection(uris)
                        d.dismiss()
                        providerHelper.setResultAndFinish(images)
                    }
                }
            }.launch(providerHelper.getGalleryMimeTypes())
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
                    loadProvider(imageProvider)
                } else {
                    // If we weren't granted the permission, check to see if we should show
                    // rationale for the permission.
                    showDialogToAcceptPermissions()
                }
                return
            }
        }

    }

    private val startSettingsForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (havePermission()) {
                replaceFragment(ImageProviderFragment.newInstance())
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