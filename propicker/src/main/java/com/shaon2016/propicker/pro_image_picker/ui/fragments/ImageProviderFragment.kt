/*
 * Copyright (c) 2020.
 * @author Md Ashiqul Islam
 * @since 2020/10/22
 */

package com.shaon2016.propicker.pro_image_picker.ui.fragments


import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.shaon2016.propicker.databinding.FragmentImageProviderBinding
import com.shaon2016.propicker.pro_image_picker.ProImagePicker
import com.shaon2016.propicker.pro_image_picker.image_picker_util.Cropper
import com.shaon2016.propicker.pro_image_picker.ui.ProImagePickerVM
import com.shaon2016.propicker.util.FileUtil
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


internal class ImageProviderFragment : Fragment() {
    private val TAG = "ImageProviderFragment"

    private val vm by lazy {
        ViewModelProvider(requireActivity()).get(ProImagePickerVM::class.java)
    }
    private val cropper by lazy { Cropper(activity as AppCompatActivity) }
    private var captureImageUri: Uri? = null

    private lateinit var binding: FragmentImageProviderBinding

    // CameraX
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentImageProviderBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        startCamera()

        binding.fabCamera.setOnClickListener {
            takePhoto()
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
                    val msg = "Photo capture succeeded: $captureImageUri"
                    //Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)

                    shouldImageBeCropped(captureImageUri, photoFile)

                }
            })
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun shouldImageBeCropped(savedUri: Uri?, sourceFile: File) {

        when {
            cropper.isCropEnabled() -> {
                val croppedFile = FileUtil.getImageOutputDirectory(requireContext())

                cropper.startCropUsingUCrop(sourceFile, croppedFile)

            }
            cropper.isToCompress() -> {
                lifecycleScope.launch {
                    if (savedUri != null) {
                        val uri = cropper.compress(savedUri)
                        // Deleting the saved image file
                        cropper.delete(savedUri)
                        setResult(uri, uri.toFile())

                    } else setResult(savedUri, sourceFile)
                }
            }
            else -> setResult(savedUri, sourceFile)
        }

    }

    // For Ucrop Result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        captureImageUri?.let {
            lifecycleScope.launch(Dispatchers.IO) {
                cropper.delete(it)
            }
        }


        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            val resultUri = UCrop.getOutput(data!!)

            setResult(resultUri, resultUri?.toFile())

        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            setResult(null, null)
        }
    }

    private fun setResult(imageUri: Uri?, imageFile: File?) {
        val intent = Intent()
        intent.data = imageUri
        intent.putExtra(ProImagePicker.EXTRA_CAPTURE_IMAGE_FILE, imageFile)
        requireActivity().setResult(RESULT_OK, intent)
        requireActivity().finish()
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