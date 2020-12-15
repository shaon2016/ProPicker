/*
 * Copyright (c) 2020.
 * @author Md Ashiqul Islam
 * @since 2020/10/22
 */

package com.shaon2016.propicker.pro_image_picker.ui

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.impl.PreviewConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.shaon2016.propicker.R
import com.shaon2016.propicker.pro_image_picker.ProviderHelper
import com.shaon2016.propicker.util.FileUtil
import kotlinx.android.synthetic.main.fragment_image_provider.*
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


internal class ImageProviderFragment : Fragment() {
    private val TAG = "ImageProviderFragment"

    private val providerHelper by lazy { ProviderHelper(requireActivity() as AppCompatActivity) }

    private var captureImageUri: Uri? = null

    // CameraX
    private var imageCapture: ImageCapture? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraProvider: ProcessCameraProvider

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_image_provider, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupCamera()

        view.findViewById<ImageView>(R.id.fabCamera).setOnClickListener {
            takePhoto()
        }
        view.findViewById<ImageView>(R.id.flipCamera).setOnClickListener {
            flipCamera()
        }


        cameraExecutor = Executors.newSingleThreadExecutor()

    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = FileUtil.getImageOutputDirectory(requireContext())

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {

                    captureImageUri = Uri.fromFile(photoFile)

                    captureImageUri?.let {
                        lifecycleScope.launch {
                            providerHelper.performCameraOperation(captureImageUri!!)

                            val msg = "Photo capture succeeded: $captureImageUri"
                            //Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                            Log.d(TAG, msg)
                        }
                    }
                }
            })
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraProviderFuture.get()

            // Select lensFacing depending on the available cameras
            lensFacing = when {
                hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
                else -> throw IllegalStateException("Back and front camera are unavailable")
            }

            // Enable or disable switching between cameras
            updateCameraSwitchButton()

            // Build and bind the camera use cases
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases() {
        // Preview
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(viewFinder.surfaceProvider)
        }

        imageCapture = ImageCapture.Builder().build()

        // Select back camera as a default
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll()

            // Bind use cases to camera
            val cm = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture
            )

            initFlash(cm)

        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    /** Returns true if the device has an available back camera. False otherwise */
    private fun hasBackCamera(): Boolean {
        return cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    /** Returns true if the device has an available front camera. False otherwise */
    private fun hasFrontCamera(): Boolean {
        return cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }

    /** Enabled or disabled a button to switch cameras depending on the available cameras */
    private fun updateCameraSwitchButton() {
        val switchCamerasButton = view?.findViewById<ImageView>(R.id.flipCamera)
        try {
            switchCamerasButton?.isEnabled = hasBackCamera() && hasFrontCamera()
        } catch (exception: CameraInfoUnavailableException) {
            switchCamerasButton?.isEnabled = false
        }
    }

    private fun flipCamera() {
        lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        // Re-bind use cases to update selected camera
        bindCameraUseCases()
    }

    private fun initFlash(camera: Camera) {
        val btnFlash = view?.findViewById<ImageView>(R.id.btnFlash)

        if (camera.cameraInfo.hasFlashUnit()) {
            btnFlash?.visibility = View.VISIBLE

            btnFlash?.setOnClickListener {
                camera.cameraControl.enableTorch(camera.cameraInfo.torchState.value == TorchState.OFF)
            }
        } else btnFlash?.visibility = View.GONE

        camera.cameraInfo.torchState.observe(viewLifecycleOwner, Observer { torchState ->
            if (torchState == TorchState.OFF) {
                btnFlash?.setImageResource(R.drawable.ic_baseline_flash_on_24)
            } else {
                btnFlash?.setImageResource(R.drawable.ic_baseline_flash_off_24)
            }
        })
    }

    // For Ucrop Result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        lifecycleScope.launch {
            providerHelper.handleUCropResult(requestCode, resultCode, data, captureImageUri)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        @JvmStatic
        fun newInstance() = ImageProviderFragment()
    }
}