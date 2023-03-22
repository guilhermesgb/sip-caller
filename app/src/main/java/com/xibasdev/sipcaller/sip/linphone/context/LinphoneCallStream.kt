package com.xibasdev.sipcaller.sip.linphone.context

import com.xibasdev.sipcaller.sip.linphone.context.LinphoneCallStreamDirection.DISABLED

data class LinphoneCallStream(
    val codecName: String = "",
    val clockRateHz: Int = -1,
    val channelsNumber: Int = -1,
    val direction: LinphoneCallStreamDirection = DISABLED
)
