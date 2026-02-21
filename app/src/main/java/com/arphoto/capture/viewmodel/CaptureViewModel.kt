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
import com.arphoto.capture.data.MeasurementResult
import com.google.gson.GsonBuilder
import com.arphoto.capture.network.UploadManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class CaptureViewModel(application: Application) : AndroidViewModel(application) {

    private val arCoreManager = ARCoreManager(application)
    private val frameExporter = FrameExporter(application)
    private val uploadManager = UploadManager()
    private val gson = GsonBuilder().setPrettyPrinting().create()

    private val _uiState = MutableStateFlow<CaptureUiState>(CaptureUiState.Idle)
    val uiState: StateFlow<CaptureUiState> = _uiState

    private val _selectedCategory = MutableStateFlow(AssetCategory.TRENCH)
    val selectedCategory: StateFlow<AssetCategory> = _selectedCategory

    private val _frameCount = MutableStateFlow(0)
    val frameCount: StateFlow<Int> = _frameCount

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState

    private val capturedMetadata = mutableListOf<FrameMetadata>()
    private val rgbFiles = mutableListOf<File>()
    private val depthFiles = mutableListOf<File>()

    val trackingState = arCoreManager.trackingState

    fun initializeARCore() {
        try {
            arCoreManager.initializeSession()
            arCoreManager.resume()
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

    fun uploadCapture() {
        viewModelScope.launch {
            _uploadState.value = UploadState.Uploading("Process")

            val (rgbFiles, depthFiles, metadata) = getCaptureData()

            val result = uploadManager.uploadCapture(rgbFiles, depthFiles, metadata)

            result.fold(
                onSuccess = { measurementResult ->
                    _uploadState.value = UploadState.MeasurementSuccess(measurementResult)
                },
                onFailure = { error ->
                    _uploadState.value = UploadState.Error(error.message ?: "Upload failed")
                }
            )
        }
    }

    fun classifyCapture() {
        viewModelScope.launch {
            _uploadState.value = UploadState.Uploading("Classify")

            val (rgbFiles, depthFiles, metadata) = getCaptureData()
            val result = uploadManager.classifyCapture(rgbFiles, depthFiles, metadata)

            result.fold(
                onSuccess = { json ->
                    _uploadState.value = UploadState.JsonSuccess("Classify", gson.toJson(json))
                },
                onFailure = { error ->
                    _uploadState.value = UploadState.Error(error.message ?: "Classification failed")
                }
            )
        }
    }

    fun reconstructCapture() {
        viewModelScope.launch {
            _uploadState.value = UploadState.Uploading("Reconstruct")

            val (rgbFiles, depthFiles, metadata) = getCaptureData()
            val result = uploadManager.reconstructCapture(rgbFiles, depthFiles, metadata)

            result.fold(
                onSuccess = { json ->
                    _uploadState.value = UploadState.JsonSuccess("Reconstruct", gson.toJson(json))
                },
                onFailure = { error ->
                    _uploadState.value = UploadState.Error(error.message ?: "Reconstruction failed")
                }
            )
        }
    }

    fun poseSanity() {
        viewModelScope.launch {
            _uploadState.value = UploadState.Uploading("Pose Sanity")

            val (_, _, metadata) = getCaptureData()
            val result = uploadManager.poseSanity(metadata)

            result.fold(
                onSuccess = { json ->
                    _uploadState.value = UploadState.JsonSuccess("Pose Sanity", gson.toJson(json))
                },
                onFailure = { error ->
                    _uploadState.value = UploadState.Error(error.message ?: "Pose sanity failed")
                }
            )
        }
    }

    fun resetUploadState() {
        _uploadState.value = UploadState.Idle
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

sealed class UploadState {
    object Idle : UploadState()
    data class Uploading(val operation: String) : UploadState()
    data class MeasurementSuccess(val result: MeasurementResult) : UploadState()
    data class JsonSuccess(val operation: String, val payload: String) : UploadState()
    data class Error(val message: String) : UploadState()
}
