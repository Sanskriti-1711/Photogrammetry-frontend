package com.arphoto.capture

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.arphoto.capture.ui.ApiResponseScreen
import com.arphoto.capture.ui.CaptureScreen
import com.arphoto.capture.ui.ResultsScreen
import com.arphoto.capture.viewmodel.CaptureViewModel
import com.arphoto.capture.viewmodel.UploadState

class MainActivity : ComponentActivity() {
    private val captureViewModel: CaptureViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            setupUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            setupUI()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun setupUI() {
        setContent {
            MaterialTheme {
                Surface {
                    val uploadState by captureViewModel.uploadState.collectAsState()

                    when (val state = uploadState) {
                        is UploadState.MeasurementSuccess -> {
                            ResultsScreen(
                                result = state.result,
                                onBackClick = {
                                    // Reset to capture screen
                                    captureViewModel.resetUploadState()
                                }
                            )
                        }
                        is UploadState.JsonSuccess -> {
                            ApiResponseScreen(
                                title = state.operation,
                                payload = state.payload,
                                onBackClick = { captureViewModel.resetUploadState() }
                            )
                        }
                        else -> {
                            CaptureScreen(
                                viewModel = captureViewModel,
                                onProcessClick = { captureViewModel.uploadCapture() },
                                onClassifyClick = { captureViewModel.classifyCapture() },
                                onReconstructClick = { captureViewModel.reconstructCapture() },
                                onPoseSanityClick = { captureViewModel.poseSanity() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        captureViewModel.onResume()
    }

    override fun onPause() {
        captureViewModel.onPause()
        super.onPause()
    }
}
