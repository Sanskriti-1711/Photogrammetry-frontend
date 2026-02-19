package com.arphoto.capture.data

import com.google.gson.annotations.SerializedName

data class MeasurementResult(
    @SerializedName("category")
    val category: String,

    @SerializedName("depth_m")
    val depthM: Double?,

    @SerializedName("width_m")
    val widthM: Double?,

    @SerializedName("diameter_m")
    val diameterM: Double?,

    @SerializedName("length_m")
    val lengthM: Double?,

    @SerializedName("L_m")
    val lM: Double?,

    @SerializedName("W_m")
    val wM: Double?,

    @SerializedName("H_m")
    val hM: Double?,

    @SerializedName("confidence")
    val confidence: String,

    @SerializedName("point_count")
    val pointCount: Int
)
