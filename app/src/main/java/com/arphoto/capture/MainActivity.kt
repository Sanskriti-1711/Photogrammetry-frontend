package com.arphoto.capture

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arphoto.capture.ui.CaptureScreen
import com.arphoto.capture.ui.ResultsScreen
import com.arphoto.capture.viewmodel.CaptureViewModel
import com.arphoto.capture.viewmodel.UploadState

class MainActivity : ComponentActivity() {

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
                    val viewModel: CaptureViewModel = viewModel()
                    val uploadState by viewModel.uploadState.collectAsState()

                    when (val state = uploadState) {
                        is UploadState.Success -> {
                            ResultsScreen(
                                result = state.result,
                                onBackClick = {
                                    // Reset to capture screen
                                    viewModel.resetUploadState()
                                }
                            )
                        }
                        else -> {
                            CaptureScreen(
                                viewModel = viewModel,
                                onUploadClick = {
                                    viewModel.uploadCapture()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
