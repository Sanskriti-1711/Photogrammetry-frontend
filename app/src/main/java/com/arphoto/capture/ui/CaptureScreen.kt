package com.arphoto.capture.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arphoto.capture.viewmodel.CaptureUiState
import com.arphoto.capture.viewmodel.CaptureViewModel
import com.arphoto.capture.viewmodel.UploadState

@Composable
fun CaptureScreen(
    viewModel: CaptureViewModel = viewModel(),
    onProcessClick: () -> Unit,
    onClassifyClick: () -> Unit,
    onReconstructClick: () -> Unit,
    onPoseSanityClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val frameCount by viewModel.frameCount.collectAsState()
    val isCapturing by viewModel.isCapturing.collectAsState()
    val trackingState by viewModel.trackingState.collectAsState()
    val uploadState by viewModel.uploadState.collectAsState()
    val previewBitmap by viewModel.previewBitmap.collectAsState()
    val arCoreError by viewModel.arCoreError.collectAsState()
    val arCoreAvailable by viewModel.arCoreAvailable.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initializeARCore()
    }

    // Continuously update tracking/preview and capture frames when recording.
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.processFrame()
            kotlinx.coroutines.delay(100)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (!arCoreAvailable) {
            CameraFallbackPreview(modifier = Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = "ARCore not available on this device. Preview only mode.",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else if (previewBitmap != null) {
            Image(
                bitmap = previewBitmap!!.asImageBitmap(),
                contentDescription = "AR Camera Preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    text = "Start Capture to show camera preview",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }

        // UI Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Category selector at top
            if (!isCapturing) {
                CategorySelector(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { viewModel.selectCategory(it) }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Controls at bottom
            CaptureControls(
                isCapturing = isCapturing,
                frameCount = frameCount,
                trackingState = trackingState,
                arCaptureEnabled = arCoreAvailable,
                onToggleCapture = { viewModel.toggleCapture() },
                onProcess = onProcessClick,
                onClassify = onClassifyClick,
                onReconstruct = onReconstructClick,
                onPoseSanity = onPoseSanityClick,
                showActionButtons = uiState is CaptureUiState.ReadyToUpload,
                isBusy = uploadState is UploadState.Uploading,
                busyOperation = (uploadState as? UploadState.Uploading)?.operation
            )
        }

        // Error snackbar
        if (uiState is CaptureUiState.Error) {
            Snackbar(
                modifier = Modifier.padding(16.dp)
            ) {
                Text((uiState as CaptureUiState.Error).message)
            }
        }

        if (uploadState is UploadState.Error) {
            Snackbar(
                modifier = Modifier.padding(16.dp)
            ) {
                Text((uploadState as UploadState.Error).message)
            }
        }

        if (arCoreError.isNotBlank()) {
            Snackbar(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(arCoreError)
            }
        }
    }
}
