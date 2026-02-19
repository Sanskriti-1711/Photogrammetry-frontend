package com.arphoto.capture.network

import com.arphoto.capture.data.MeasurementResult
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface BackendApi {

    @Multipart
    @POST("/api/measure")
    suspend fun uploadCapture(
        @Part rgbFrames: List<MultipartBody.Part>,
        @Part depthMaps: List<MultipartBody.Part>,
        @Part metadata: MultipartBody.Part
    ): Response<MeasurementResult>
}
