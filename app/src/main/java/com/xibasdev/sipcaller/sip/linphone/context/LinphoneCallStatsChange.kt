package com.xibasdev.sipcaller.sip.linphone.context

data class LinphoneCallStatsChange(
    val callId: String,
    val audioStats: LinphoneCallStats,
    val videoStats: LinphoneCallStats
)
