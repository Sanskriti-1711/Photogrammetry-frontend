package com.arphoto.capture.arcore

import android.content.Context
import android.media.Image
import com.google.ar.core.*
import com.arphoto.capture.data.FrameMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ARCoreManager(private val context: Context) {

    private var session: Session? = null
    private val _trackingState = MutableStateFlow("NOT_TRACKING")
    val trackingState: StateFlow<String> = _trackingState

    data class CapturedFrame(
        val rgbImage: Image,
        val depthImage: Image,
        val metadata: FrameMetadata
    )

    fun initializeSession() {
        if (session != null) return
        session = Session(context).apply {
            val config = Config(this).apply {
                depthMode = Config.DepthMode.AUTOMATIC
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                focusMode = Config.FocusMode.AUTO
            }
            configure(config)
        }
    }

    fun update(): Frame? {
        val session = session ?: return null

        return try {
            session.update()
        } catch (e: Exception) {
            null
        }
    }

    fun captureFrame(frame: Frame): CapturedFrame? {
        val camera = frame.camera

        // Only capture if tracking is good
        if (camera.trackingState != TrackingState.TRACKING) {
            _trackingState.value = camera.trackingState.name
            return null
        }
        _trackingState.value = "TRACKING"

        // Get RGB image
        val rgbImage = try {
            frame.acquireCameraImage()
        } catch (e: Exception) {
            return null
        }

        // Get depth image (16-bit)
        val depthImage = try {
            frame.acquireDepthImage16Bits()
        } catch (e: Exception) {
            rgbImage.close()
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

    fun pause() {
        try {
            session?.pause()
        } catch (_: Exception) {
        }
    }

    fun resume() {
        try {
            session?.resume()
        } catch (_: Exception) {
        }
    }

    fun destroy() {
        session?.close()
        session = null
    }
}
