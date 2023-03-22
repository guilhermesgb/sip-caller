package com.xibasdev.sipcaller.sip.calling.streams

/**
 * Snapshot of the state of upload/download bandwidth statistics for an arbitrary media property.
 */
data class PropertyStatistics(
    val uploadBandwidth: Float = 0f,
    val downloadBandwidth: Float = 0f
)
