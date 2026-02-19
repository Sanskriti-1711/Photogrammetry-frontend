package com.arphoto.capture.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arphoto.capture.arcore.ARCoreManager
import com.arphoto.capture.arcore.DepthProcessor
import com.arphoto.capture.arcore.FrameExporter
import com.arphoto.capture.data.AssetCategory
import com.arphoto.capture.data.CaptureMetadata
import com.arphoto.capture.data.FrameMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class CaptureViewModel(application: Application) : AndroidViewModel(application) {

    private val arCoreManager = ARCoreManager(application)
    private val frameExporter = FrameExporter(application)

    private val _uiState = MutableStateFlow<CaptureUiState>(CaptureUiState.Idle)
    val uiState: StateFlow<CaptureUiState> = _uiState

    private val _selectedCategory = MutableStateFlow(AssetCategory.TRENCH)
    val selectedCategory: StateFlow<AssetCategory> = _selectedCategory

    private val _frameCount = MutableStateFlow(0)
    val frameCount: StateFlow<Int> = _frameCount

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing

    private val capturedMetadata = mutableListOf<FrameMetadata>()
    private val rgbFiles = mutableListOf<File>()
    private val depthFiles = mutableListOf<File>()

    val trackingState = arCoreManager.trackingState

    fun initializeARCore() {
        try {
            arCoreManager.initializeSession()
            _uiState.value = CaptureUiState.Ready
        } catch (e: Exception) {
            _uiState.value = CaptureUiState.Error(e.message ?: "ARCore init failed")
        }
    }

    fun selectCategory(category: AssetCategory) {
        _selectedCategory.value = category
    }

    fun toggleCapture() {
        _isCapturing.value = !_isCapturing.value

        if (!_isCapturing.value && capturedMetadata.isNotEmpty()) {
            // Stopped capturing - ready to upload
            _uiState.value = CaptureUiState.ReadyToUpload(
                frameCount = _frameCount.value,
                category = _selectedCategory.value
            )
        } else if (_isCapturing.value) {
            // Started capturing - clear previous data
            clearCaptureData()
            _uiState.value = CaptureUiState.Capturing
        }
    }

    fun processFrame() {
        if (!_isCapturing.value) return

        viewModelScope.launch {
            val frame = arCoreManager.update() ?: return@launch
            val capturedFrame = arCoreManager.captureFrame(frame) ?: return@launch

            val frameNum = _frameCount.value

            // Apply confidence masking
            val maskedDepth = DepthProcessor.applyConfidenceMask(
                capturedFrame.depthImage,
                frame
            )

            // Save RGB
            val rgbFile = frameExporter.saveRgbImage(
                capturedFrame.rgbImage,
                frameNum
            )

            // Save depth
            val depthFile = frameExporter.saveDepthImage(
                maskedDepth,
                capturedFrame.depthImage.width,
                capturedFrame.depthImage.height,
                frameNum
            )

            // Store metadata
            capturedMetadata.add(capturedFrame.metadata)
            rgbFiles.add(rgbFile)
            depthFiles.add(depthFile)

            // Clean up images
            capturedFrame.rgbImage.close()
            capturedFrame.depthImage.close()

            // Update count
            _frameCount.value = frameNum + 1
        }
    }

    fun getCaptureData(): Triple<List<File>, List<File>, CaptureMetadata> {
        val metadata = CaptureMetadata(
            category = _selectedCategory.value.toApiString(),
            frames = capturedMetadata
        )
        return Triple(rgbFiles, depthFiles, metadata)
    }

    private fun clearCaptureData() {
        capturedMetadata.clear()
        rgbFiles.clear()
        depthFiles.clear()
        _frameCount.value = 0
        frameExporter.clearCache()
    }

    fun onPause() {
        arCoreManager.pause()
    }

    fun onResume() {
        arCoreManager.resume()
    }

    override fun onCleared() {
        super.onCleared()
        arCoreManager.destroy()
    }
}

sealed class CaptureUiState {
    object Idle : CaptureUiState()
    object Ready : CaptureUiState()
    object Capturing : CaptureUiState()
    data class ReadyToUpload(val frameCount: Int, val category: AssetCategory) : CaptureUiState()
    data class Error(val message: String) : CaptureUiState()
}
