package com.arphoto.capture.network

import com.arphoto.capture.data.MeasurementResult
import com.google.gson.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap

interface BackendApi {

    @Multipart
    @POST("/process")
    suspend fun uploadCapture(
        @Part files: List<MultipartBody.Part>,
        @PartMap formFields: Map<String, @JvmSuppressWildcards RequestBody>
    ): Response<MeasurementResult>

    @Multipart
    @POST("/classify")
    suspend fun classifyCapture(
        @Part files: List<MultipartBody.Part>,
        @PartMap formFields: Map<String, @JvmSuppressWildcards RequestBody>
    ): Response<JsonObject>

    @Multipart
    @POST("/reconstruct")
    suspend fun reconstructCapture(
        @Part files: List<MultipartBody.Part>,
        @PartMap formFields: Map<String, @JvmSuppressWildcards RequestBody>
    ): Response<JsonObject>

    @Multipart
    @POST("/pose_sanity")
    suspend fun poseSanity(
        @PartMap formFields: Map<String, @JvmSuppressWildcards RequestBody>
    ): Response<JsonObject>
}
