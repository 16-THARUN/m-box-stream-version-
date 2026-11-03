package com.mediaplayer.data.model

data class ResolvedStream(
    val url: String,
    val codec: String,
    val bitrateKbps: Int,
    val sampleRateHz: Int? = null,
    val container: String,
    val lossy: Boolean = true,
    val expiresAtMs: Long? = null
) {
    fun toQualityLabel(sourceDisplayName: String): String {
        val sampleRateStr = if (sampleRateHz != null && sampleRateHz > 0) " · ${sampleRateHz / 1000f} kHz" else ""
        val qualityType = if (lossy) "Lossy" else "Lossless"
        return "$sourceDisplayName · ${codec.uppercase()} $bitrateKbps kbps$sampleRateStr · $qualityType"
    }
}
