package com.xibasdev.sipcaller.sip.calling.streams

/**
 * Snapshot of the state of all observable statistics for an arbitrary media stream.
 */
data class StreamStatistics(
    val fps: Float = 0f,
    val media: PropertyStatistics = PropertyStatistics(),
    val control: PropertyStatistics = PropertyStatistics()
)
