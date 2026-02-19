package com.arphoto.capture.data

enum class AssetCategory {
    TRENCH,
    MANHOLE,
    DUCT,
    HANDHOLE;

    fun toApiString(): String = name.lowercase()

    fun getDisplayName(): String = when(this) {
        TRENCH -> "🟫 Trench"
        MANHOLE -> "🟢 Manhole"
        DUCT -> "🔵 Duct"
        HANDHOLE -> "🟨 Handhole"
    }
}
