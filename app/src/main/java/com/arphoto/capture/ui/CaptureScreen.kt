package com.arphoto.capture.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    LaunchedEffect(Unit) {
        viewModel.initializeARCore()
    }

    // Process frames when capturing
    LaunchedEffect(isCapturing) {
        if (isCapturing) {
            while (isCapturing) {
                viewModel.processFrame()
                kotlinx.coroutines.delay(100) // Capture ~10 fps
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // TODO: AR Camera Preview will go here (Step 3b)
        // For now, show placeholder
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(
                text = "AR Camera Preview",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium
            )
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
    }
}
