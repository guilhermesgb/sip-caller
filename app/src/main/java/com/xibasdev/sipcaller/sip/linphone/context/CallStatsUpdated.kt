package com.xibasdev.sipcaller.sip.linphone.context

data class CallStatsUpdated(
    val stream: LinphoneCallStream = LinphoneCallStream(),
    val dataDownloadBandwidthKbps: Float = 0.0f,
    val dataUploadBandwidthKbps: Float = 0.0f,
    val controlDownloadBandwidthKbps: Float = 0.0f,
    val controlUploadBandwidthKbps: Float = 0.0f
) : LinphoneCallStats
