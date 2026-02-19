package com.arphoto.capture.data

import com.google.gson.annotations.SerializedName

data class FrameMetadata(
    @SerializedName("intrinsics")
    val intrinsics: FloatArray,  // [fx, fy, cx, cy]

    @SerializedName("pose")
    val pose: FloatArray,        // 4x4 matrix flattened to [16]

    @SerializedName("timestamp")
    val timestamp: Long
)

data class CaptureMetadata(
    @SerializedName("category")
    val category: String,

    @SerializedName("frames")
    val frames: List<FrameMetadata>
)
