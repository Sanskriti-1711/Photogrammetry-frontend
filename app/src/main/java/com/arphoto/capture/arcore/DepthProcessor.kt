package com.arphoto.capture.arcore

import android.media.Image
import com.google.ar.core.Frame
import java.nio.ByteOrder

object DepthProcessor {

    private const val CONFIDENCE_THRESHOLD = 128 // [0..255]

    /**
     * Apply confidence masking to depth image.
     * Sets low-confidence pixels to 0.
     */
    fun applyConfidenceMask(
        depthImage: Image,
        frame: Frame
    ): ShortArray {
        val depthArray = extractDepthArray(depthImage)

        // Get confidence image
        val confidenceImage = try {
            frame.acquireRawDepthConfidenceImage()
        } catch (e: Exception) {
            // If confidence not available, return original depth
            return depthArray
        }

        val width = depthImage.width
        val height = depthImage.height
        val maskWidth = minOf(width, confidenceImage.width)
        val maskHeight = minOf(height, confidenceImage.height)
        val confidencePlane = confidenceImage.planes[0]
        val confidenceBuffer = confidencePlane.buffer
        val confidenceRowStride = confidencePlane.rowStride
        val confidencePixelStride = confidencePlane.pixelStride

        // Mask low-confidence pixels
        for (y in 0 until maskHeight) {
            for (x in 0 until maskWidth) {
                val idx = y * width + x
                val confOffset = y * confidenceRowStride + x * confidencePixelStride
                val confidence = confidenceBuffer.get(confOffset).toInt() and 0xFF
                if (confidence < CONFIDENCE_THRESHOLD) {
                    depthArray[idx] = 0
                }
            }
        }

        confidenceImage.close()
        return depthArray
    }

    private fun extractDepthArray(depthImage: Image): ShortArray {
        val width = depthImage.width
        val height = depthImage.height
        val out = ShortArray(width * height)
        val plane = depthImage.planes[0]
        val depthBuffer = plane.buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride

        for (y in 0 until height) {
            val rowStart = y * rowStride
            for (x in 0 until width) {
                val depthOffset = rowStart + x * pixelStride
                out[y * width + x] = depthBuffer.getShort(depthOffset)
            }
        }
        return out
    }
}
