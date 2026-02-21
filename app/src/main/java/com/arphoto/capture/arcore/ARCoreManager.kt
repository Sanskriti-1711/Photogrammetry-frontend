package com.arphoto.capture.arcore

import android.content.Context
import android.media.Image
import com.google.ar.core.*
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.SessionPausedException
import com.google.ar.core.exceptions.TextureNotSetException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.arphoto.capture.data.FrameMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ARCoreManager(private val context: Context) {

    private var session: Session? = null
    private val _trackingState = MutableStateFlow("NOT_TRACKING")
    val trackingState: StateFlow<String> = _trackingState
    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage
    private val _isArCoreAvailable = MutableStateFlow(true)
    val isArCoreAvailable: StateFlow<Boolean> = _isArCoreAvailable

    data class CapturedFrame(
        val rgbImage: Image,
        val depthImage: Image,
        val metadata: FrameMetadata
    )

    fun initializeSession() {
        if (session != null) return
        try {
            session = Session(context).apply {
                val config = Config(this).apply {
                    depthMode = if (isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                        Config.DepthMode.AUTOMATIC
                    } else {
                        Config.DepthMode.DISABLED
                    }
                    textureUpdateMode = Config.TextureUpdateMode.EXPOSE_HARDWARE_BUFFER
                    updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                    focusMode = Config.FocusMode.AUTO
                }
                configure(config)
            }
            _isArCoreAvailable.value = true
            _errorMessage.value = ""
        } catch (e: UnavailableDeviceNotCompatibleException) {
            _isArCoreAvailable.value = false
            _trackingState.value = "ARCORE_UNAVAILABLE"
            _errorMessage.value = "ARCore not supported on this device. Showing standard camera preview only."
        } catch (e: UnavailableArcoreNotInstalledException) {
            _isArCoreAvailable.value = false
            _trackingState.value = "ARCORE_NOT_INSTALLED"
            _errorMessage.value = "Google Play Services for AR is not installed."
        } catch (e: UnavailableApkTooOldException) {
            _isArCoreAvailable.value = false
            _trackingState.value = "ARCORE_APK_OLD"
            _errorMessage.value = "Google Play Services for AR is outdated."
        } catch (e: UnavailableSdkTooOldException) {
            _isArCoreAvailable.value = false
            _trackingState.value = "ARCORE_SDK_OLD"
            _errorMessage.value = "App ARCore SDK is outdated for this device."
        } catch (e: Exception) {
            _isArCoreAvailable.value = false
            _trackingState.value = "ARCORE_INIT_FAILED"
            _errorMessage.value = "ARCore init failed: ${e.message ?: "Unknown error"}"
        }
    }

    fun update(): Frame? {
        val session = session ?: return null

        return try {
            session.update()
        } catch (e: Exception) {
            val detail = when (e) {
                is TextureNotSetException -> {
                    "Camera texture not bound. Current build needs a GL background renderer or hardware-buffer texture mode support."
                }
                is SessionPausedException -> "Session is paused. Resume ARCore before capturing."
                is CameraNotAvailableException -> "Camera not available. Close other camera apps and retry."
                else -> e.message ?: "Unknown error"
            }
            _trackingState.value = "UPDATE_FAILED"
            _errorMessage.value = "ARCore update failed (${e::class.java.simpleName}): $detail"
            null
        }
    }

    fun captureFrame(frame: Frame): CapturedFrame? {
        val camera = frame.camera

        // Only capture if tracking is good
        if (camera.trackingState != TrackingState.TRACKING) {
            val reason = if (camera.trackingState == TrackingState.PAUSED) {
                " (${camera.trackingFailureReason.name})"
            } else {
                ""
            }
            _trackingState.value = "${camera.trackingState.name}$reason"
            return null
        }
        _trackingState.value = "TRACKING"

        // Get RGB image
        val rgbImage = try {
            frame.acquireCameraImage()
        } catch (e: Exception) {
            _errorMessage.value = "RGB frame unavailable: ${e.message ?: "Unknown error"}"
            return null
        }

        // Get depth image (16-bit)
        val depthImage = try {
            frame.acquireDepthImage16Bits()
        } catch (e: Exception) {
            rgbImage.close()
            _errorMessage.value = "Depth frame unavailable: ${e.message ?: "Unsupported on this device"}"
            return null
        }

        // Get camera intrinsics
        val intrinsics = camera.imageIntrinsics
        val intrinsicsArray = floatArrayOf(
            intrinsics.focalLength[0],   // fx
            intrinsics.focalLength[1],   // fy
            intrinsics.principalPoint[0], // cx
            intrinsics.principalPoint[1]  // cy
        )

        // Get camera pose (4x4 matrix)
        val poseMatrix = FloatArray(16)
        camera.pose.toMatrix(poseMatrix, 0)

        val metadata = FrameMetadata(
            intrinsics = intrinsicsArray,
            pose = poseMatrix,
            timestamp = frame.timestamp
        )

        return CapturedFrame(rgbImage, depthImage, metadata)
    }

    fun capturePreviewImage(frame: Frame): Image? {
        val camera = frame.camera
        if (camera.trackingState != TrackingState.TRACKING) {
            val reason = if (camera.trackingState == TrackingState.PAUSED) {
                " (${camera.trackingFailureReason.name})"
            } else {
                ""
            }
            _trackingState.value = "${camera.trackingState.name}$reason"
        } else {
            _trackingState.value = "TRACKING"
        }

        return try {
            _errorMessage.value = ""
            frame.acquireCameraImage()
        } catch (e: Exception) {
            _errorMessage.value = "Preview frame unavailable: ${e.message ?: "Unknown error"}"
            null
        }
    }

    fun pause() {
        try {
            session?.pause()
        } catch (_: Exception) {
        }
    }

    fun resume() {
        val currentSession = session ?: return
        try {
            currentSession.resume()
            _errorMessage.value = ""
        } catch (e: Exception) {
            _trackingState.value = "RESUME_FAILED"
            _errorMessage.value = "ARCore resume failed: ${e.message ?: "Unknown error"}"
            throw e
        }
    }

    fun destroy() {
        session?.close()
        session = null
    }
}
