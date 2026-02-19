package com.arphoto.capture.network

import com.arphoto.capture.data.CaptureMetadata
import com.arphoto.capture.data.MeasurementResult
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class UploadManager {

    private val api = ApiConfig.api
    private val gson = Gson()

    suspend fun uploadCapture(
        rgbFiles: List<File>,
        depthFiles: List<File>,
        metadata: CaptureMetadata
    ): Result<MeasurementResult> {
        return try {
            // Build RGB parts
            val rgbParts = rgbFiles.map { file ->
                val requestBody = file.asRequestBody("image/jpeg".toMediaType())
                MultipartBody.Part.createFormData("rgb", file.name, requestBody)
            }

            // Build depth parts
            val depthParts = depthFiles.map { file ->
                val requestBody = file.asRequestBody("image/png".toMediaType())
                MultipartBody.Part.createFormData("depth", file.name, requestBody)
            }

            // Build metadata part
            val metadataJson = gson.toJson(metadata)
            val metadataBody = metadataJson.toRequestBody("application/json".toMediaType())
            val metadataPart = MultipartBody.Part.createFormData(
                "metadata",
                "metadata.json",
                metadataBody
            )

            // Execute upload
            val response = api.uploadCapture(rgbParts, depthParts, metadataPart)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Upload failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
