package com.arphoto.capture.network

import com.arphoto.capture.data.CaptureMetadata
import com.arphoto.capture.data.FrameMetadata
import com.arphoto.capture.data.MeasurementResult
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
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
            validateRgbdPayload(rgbFiles, depthFiles, metadata)
            val fileParts = buildRgbdFileParts(rgbFiles, depthFiles)
            val formFields = buildMetadataFormFields(metadata, includeCategory = true)
            toResult(api.uploadCapture(files = fileParts, formFields = formFields), "Upload")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun classifyCapture(
        rgbFiles: List<File>,
        depthFiles: List<File>,
        metadata: CaptureMetadata
    ): Result<JsonObject> {
        return try {
            validateRgbdPayload(rgbFiles, depthFiles, metadata)
            val fileParts = buildRgbdFileParts(rgbFiles, depthFiles)
            val formFields = buildMetadataFormFields(metadata, includeCategory = false)
            toResult(api.classifyCapture(files = fileParts, formFields = formFields), "Classify")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reconstructCapture(
        rgbFiles: List<File>,
        depthFiles: List<File>,
        metadata: CaptureMetadata
    ): Result<JsonObject> {
        return try {
            validateRgbdPayload(rgbFiles, depthFiles, metadata)
            val fileParts = buildRgbdFileParts(rgbFiles, depthFiles)
            val formFields = buildMetadataFormFields(metadata, includeCategory = true)
            toResult(api.reconstructCapture(files = fileParts, formFields = formFields), "Reconstruct")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun poseSanity(metadata: CaptureMetadata): Result<JsonObject> {
        return try {
            if (metadata.frames.isEmpty()) {
                return Result.failure(IllegalArgumentException("No capture frames available for pose sanity."))
            }
            val formFields = buildMetadataFormFields(metadata, includeCategory = false)
            toResult(api.poseSanity(formFields = formFields), "Pose sanity")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun validateRgbdPayload(
        rgbFiles: List<File>,
        depthFiles: List<File>,
        metadata: CaptureMetadata
    ) {
        if (rgbFiles.isEmpty() || depthFiles.isEmpty() || metadata.frames.isEmpty()) {
            throw IllegalArgumentException("No capture frames available to upload.")
        }
        if (rgbFiles.size != depthFiles.size || rgbFiles.size != metadata.frames.size) {
            throw IllegalArgumentException(
                "Frame count mismatch (rgb=${rgbFiles.size}, depth=${depthFiles.size}, metadata=${metadata.frames.size})."
            )
        }
    }

    private fun buildRgbdFileParts(
        rgbFiles: List<File>,
        depthFiles: List<File>
    ): List<MultipartBody.Part> {
        val fileParts = mutableListOf<MultipartBody.Part>()
        rgbFiles.forEachIndexed { i, file ->
            val requestBody = file.asRequestBody("image/jpeg".toMediaType())
            fileParts += MultipartBody.Part.createFormData("rgb_$i", file.name, requestBody)
        }
        depthFiles.forEachIndexed { i, file ->
            val requestBody = file.asRequestBody("image/png".toMediaType())
            fileParts += MultipartBody.Part.createFormData("depth_$i", file.name, requestBody)
        }
        return fileParts
    }

    private fun buildMetadataFormFields(
        metadata: CaptureMetadata,
        includeCategory: Boolean
    ): MutableMap<String, RequestBody> {
        val formFields = mutableMapOf<String, RequestBody>()
        val intrinsics = buildIntrinsicsMatrix(metadata.frames.first())
        val poses = metadata.frames.map { frame -> buildPoseMatrix(frame) }

        formFields["intrinsics"] = gson.toJson(intrinsics)
            .toRequestBody("text/plain".toMediaType())
        formFields["poses"] = gson.toJson(poses)
            .toRequestBody("text/plain".toMediaType())
        if (includeCategory && metadata.category.isNotBlank()) {
            formFields["category"] = metadata.category
                .toRequestBody("text/plain".toMediaType())
        }
        return formFields
    }

    private fun <T> toResult(response: Response<T>, action: String): Result<T> {
        if (response.isSuccessful && response.body() != null) {
            return Result.success(response.body()!!)
        }
        val backendMessage = runCatching { response.errorBody()?.string() }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: response.message()
        return Result.failure(Exception("$action failed: ${response.code()} $backendMessage"))
    }

    private fun buildIntrinsicsMatrix(frame: FrameMetadata): List<List<Float>> {
        val fx = frame.intrinsics.getOrNull(0) ?: 0f
        val fy = frame.intrinsics.getOrNull(1) ?: 0f
        val cx = frame.intrinsics.getOrNull(2) ?: 0f
        val cy = frame.intrinsics.getOrNull(3) ?: 0f
        return listOf(
            listOf(fx, 0f, cx),
            listOf(0f, fy, cy),
            listOf(0f, 0f, 1f)
        )
    }

    private fun buildPoseMatrix(frame: FrameMetadata): List<List<Float>> {
        val p = frame.pose
        if (p.size < 16) {
            return listOf(
                listOf(1f, 0f, 0f, 0f),
                listOf(0f, 1f, 0f, 0f),
                listOf(0f, 0f, 1f, 0f),
                listOf(0f, 0f, 0f, 1f)
            )
        }
        // ARCore Pose.toMatrix() returns a column-major 4x4 matrix.
        // Backend expects row-major nested lists, so transpose on serialization.
        return listOf(
            listOf(p[0], p[4], p[8], p[12]),
            listOf(p[1], p[5], p[9], p[13]),
            listOf(p[2], p[6], p[10], p[14]),
            listOf(p[3], p[7], p[11], p[15])
        )
    }
}
