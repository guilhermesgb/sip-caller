package com.xibasdev.sipcaller.sip.calling.features

/**
 * A snapshot of the current state of all call features for an arbitrary call.
 *
 * Currently-supported call features are: screenshot capture and recording capture. Both refer to
 *   capture of data from the remote party's video stream. Audio data may be included in the
 *   captured video recording.
 */
data class CallFeatures(
    val microphone: CallFeature = CallFeature(),
    val speaker: CallFeature = CallFeature(),
    val camera: CallFeature = CallFeature(),
    val screenshot: CallFeature = CallFeature(),
    val recording: CallFeature = CallFeature()
)
