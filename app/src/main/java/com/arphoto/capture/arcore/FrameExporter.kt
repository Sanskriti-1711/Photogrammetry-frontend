package com.arphoto.capture.arcore

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ShortBuffer

class FrameExporter(private val context: Context) {

    private val cacheDir = context.cacheDir

    /**
     * Save RGB image as JPEG
     */
    fun saveRgbImage(image: Image, frameNumber: Int): File {
        val rgbFile = File(cacheDir, "frame_${frameNumber}.jpg")

        // Convert YUV_420_888 to JPEG
        val yuvImage = YuvImage(
            imageToByteArray(image),
            ImageFormat.NV21,
            image.width,
            image.height,
            null
        )

        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(
            Rect(0, 0, image.width, image.height),
            95,  // JPEG quality
            out
        )

        val jpegBytes = out.toByteArray()
        rgbFile.writeBytes(jpegBytes)

        return rgbFile
    }

    /**
     * Save depth image as 16-bit PNG
     */
    fun saveDepthImage(depthBuffer: ShortBuffer, width: Int, height: Int, frameNumber: Int): File {
        val depthFile = File(cacheDir, "depth_${frameNumber}.png")

        // Convert ShortBuffer to Bitmap (grayscale 16-bit)
        val pixels = IntArray(width * height)
        depthBuffer.rewind()

        for (i in pixels.indices) {
            val depth = depthBuffer.get(i).toInt() and 0xFFFF
            // Scale to 8-bit for PNG (multiply by 255/max_depth)
            val scaled = (depth * 255 / 8000).coerceIn(0, 255)
            pixels[i] = (0xFF shl 24) or (scaled shl 16) or (scaled shl 8) or scaled
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

        depthFile.outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        return depthFile
    }

    private fun imageToByteArray(image: Image): ByteArray {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        return nv21
    }

    fun clearCache() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }
}
