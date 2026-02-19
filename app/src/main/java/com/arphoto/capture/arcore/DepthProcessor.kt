package com.arphoto.capture.arcore

import android.media.Image
import com.google.ar.core.Frame
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer

object DepthProcessor {

    private const val CONFIDENCE_THRESHOLD = 0.5f

    /**
     * Apply confidence masking to depth image.
     * Sets low-confidence pixels to 0.
     */
    fun applyConfidenceMask(
        depthImage: Image,
        frame: Frame
    ): ShortBuffer {
        val depthBuffer = depthImage.planes[0].buffer.asShortBuffer()
        val depthArray = ShortArray(depthBuffer.capacity())
        depthBuffer.get(depthArray)
        depthBuffer.rewind()

        // Get confidence image
        val confidenceImage = try {
            frame.acquireRawDepthConfidenceImage()
        } catch (e: Exception) {
            // If confidence not available, return original depth
            return depthBuffer
        }

        val confidenceBuffer = confidenceImage.planes[0].buffer

        // Mask low-confidence pixels
        for (i in depthArray.indices) {
            val confidence = (confidenceBuffer.get(i).toInt() and 0xFF) / 255f
            if (confidence < CONFIDENCE_THRESHOLD) {
                depthArray[i] = 0  // Mask pixel
            }
        }

        confidenceImage.close()

        // Return masked depth as ShortBuffer
        val maskedBuffer = ByteBuffer.allocateDirect(depthArray.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
        maskedBuffer.put(depthArray)
        maskedBuffer.rewind()

        return maskedBuffer
    }
}
