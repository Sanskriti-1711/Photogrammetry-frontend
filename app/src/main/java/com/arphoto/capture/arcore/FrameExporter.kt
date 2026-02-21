package com.arphoto.capture.arcore

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.OutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream

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
     * Save depth image as true 16-bit grayscale PNG (millimeters).
     */
    fun saveDepthImage(depthValues: ShortArray, width: Int, height: Int, frameNumber: Int): File {
        val depthFile = File(cacheDir, "depth_${frameNumber}.png")
        val pixelCount = width * height
        require(depthValues.size >= pixelCount) {
            "Depth buffer is smaller than expected image dimensions."
        }
        depthFile.outputStream().use { out ->
            writeGray16Png(out, depthValues, width, height)
        }
        return depthFile
    }

    private fun writeGray16Png(out: OutputStream, depthValues: ShortArray, width: Int, height: Int) {
        val pngSignature = byteArrayOf(
            137.toByte(), 80, 78, 71, 13, 10, 26, 10
        )
        val rawScanlineData = ByteArrayOutputStream((width * 2 + 1) * height)
        for (y in 0 until height) {
            rawScanlineData.write(0) // PNG filter type: None
            val rowOffset = y * width
            for (x in 0 until width) {
                val depthMm = depthValues[rowOffset + x].toInt() and 0xFFFF
                rawScanlineData.write((depthMm ushr 8) and 0xFF) // big-endian per PNG spec
                rawScanlineData.write(depthMm and 0xFF)
            }
        }

        val compressed = ByteArrayOutputStream(rawScanlineData.size())
        DeflaterOutputStream(compressed, Deflater(Deflater.BEST_SPEED)).use { deflater ->
            deflater.write(rawScanlineData.toByteArray())
        }

        DataOutputStream(out).use { dataOut ->
            dataOut.write(pngSignature)

            val ihdr = ByteArrayOutputStream(13)
            DataOutputStream(ihdr).use { ihdrOut ->
                ihdrOut.writeInt(width)
                ihdrOut.writeInt(height)
                ihdrOut.writeByte(16) // bit depth
                ihdrOut.writeByte(0) // grayscale
                ihdrOut.writeByte(0) // compression method
                ihdrOut.writeByte(0) // filter method
                ihdrOut.writeByte(0) // no interlace
            }
            writeChunk(dataOut, "IHDR", ihdr.toByteArray())
            writeChunk(dataOut, "IDAT", compressed.toByteArray())
            writeChunk(dataOut, "IEND", ByteArray(0))
        }
    }

    private fun writeChunk(out: DataOutputStream, type: String, data: ByteArray) {
        val typeBytes = type.toByteArray(Charsets.US_ASCII)
        out.writeInt(data.size)
        out.write(typeBytes)
        out.write(data)

        val crc = CRC32()
        crc.update(typeBytes)
        crc.update(data)
        out.writeInt(crc.value.toInt())
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
