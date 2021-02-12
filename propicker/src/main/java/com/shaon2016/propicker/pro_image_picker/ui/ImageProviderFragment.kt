/*
 * Copyright (c) 2020.
 * @author Md Ashiqul Islam
 * @since 2020/10/22
 */

package com.shaon2016.propicker.pro_image_picker.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.impl.PreviewConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


internal class ImageProviderFragment : Fragment() {
    private val TAG = "ImageProviderFragment"
    private lateinit var container: RelativeLayout
    private val providerHelper by lazy { ProviderHelper(requireActivity() as AppCompatActivity) }

    private var captureImageUri: Uri? = null

    private val pref by lazy {
        requireContext().getSharedPreferences("propicker", Context.MODE_PRIVATE)
    }

    // CameraX
    private var imageCapture: ImageCapture? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var viewFinder: PreviewView
    private var displayId: Int = -1

    private val displayManager by lazy {
        requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }


    /**
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@ImageProviderFragment.displayId) {
                Log.d(TAG, "Rotation changed: ${view.display.rotation}")
                imageCapture?.targetRotation = view.display.rotation
            }
        } ?: Unit
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_image_provider, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        container = view as RelativeLayout
        viewFinder = container.findViewById(R.id.viewFinder)

        viewFinder.post {
            setupCamera()

            updateCameraUI()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Every time the orientation of device changes, update rotation for use cases
        displayManager.registerDisplayListener(displayListener, null)

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        updateCameraUI()
        updateCameraSwitchButton()
    }

    private fun updateCameraUI() {
        container.findViewById<ImageView>(R.id.fabCamera).setOnClickListener {
            takePhoto()
        }
        container.findViewById<ImageView>(R.id.flipCamera).setOnClickListener {
            flipCamera()
        }
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = FileUtil.getImageOutputDirectory(requireContext())

        // Setup image capture metadata
        val metadata = ImageCapture.Metadata().apply {

            // Mirror image when using the front camera
            isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
        }
        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
            .setMetadata(metadata)
            .build()

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

    private var orientationEventListener: OrientationEventListener? = null

    private fun bindCameraUseCases() {
        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        Log.d(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

        val rotation = viewFinder.display.rotation


        // Preview
        val preview = Preview.Builder()
            // We request aspect ratio but no resolution
            .setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation
            .setTargetRotation(rotation)
            .build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(screenAspectRatio)
            .build()

        orientationEventListener = object : OrientationEventListener(requireContext()) {
            override fun onOrientationChanged(orientation: Int) {
                // Monitors orientation values to determine the target rotation value
                val rotation: Int = when (orientation) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }

                try {
                    if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                        if (rotation == Surface.ROTATION_0)
                            pref.edit().putBoolean("front_camera_vertical", true).apply()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                imageCapture?.targetRotation = rotation
            }
        }
        orientationEventListener?.enable()


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

    /**
     *  [androidx.camera.core.ImageAnalysisConfig] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
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
        displayManager.unregisterDisplayListener(displayListener)

    }

    override fun onStop() {
        super.onStop()

        orientationEventListener?.disable()
        orientationEventListener = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = ImageProviderFragment()

        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

    }
}