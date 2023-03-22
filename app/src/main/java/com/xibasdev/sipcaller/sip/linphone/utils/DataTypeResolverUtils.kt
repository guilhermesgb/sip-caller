package com.xibasdev.sipcaller.sip.linphone.utils

import com.xibasdev.sipcaller.sip.calling.streams.MediaStream
import com.xibasdev.sipcaller.sip.calling.streams.StreamDirection
import com.xibasdev.sipcaller.sip.calling.streams.codec.CHANNEL_TYPE_AUDIO_MONO
import com.xibasdev.sipcaller.sip.calling.streams.codec.CHANNEL_TYPE_AUDIO_STEREO
import com.xibasdev.sipcaller.sip.calling.streams.codec.CHANNEL_TYPE_VIDEO
import com.xibasdev.sipcaller.sip.calling.streams.codec.ChannelType
import com.xibasdev.sipcaller.sip.calling.streams.codec.ClockRate
import com.xibasdev.sipcaller.sip.calling.streams.codec.CodecName
import com.xibasdev.sipcaller.sip.calling.streams.codec.MediaCodec
import com.xibasdev.sipcaller.sip.calling.streams.codec.UNDEFINED_CHANNEL_TYPE
import com.xibasdev.sipcaller.sip.calling.streams.codec.UNDEFINED_CLOCK_RATE
import com.xibasdev.sipcaller.sip.calling.streams.codec.UNDEFINED_MEDIA_CODEC
import com.xibasdev.sipcaller.sip.linphone.context.LinphoneCallStream
import com.xibasdev.sipcaller.sip.linphone.context.LinphoneCallStreamDirection


internal fun resolveMediaStream(stream: LinphoneCallStream): MediaStream {
    return MediaStream(
        codec = MediaCodec(
            codecName = resolveCodecName(stream.codecName),
            clockRateHz = resolveClockRate(stream.clockRateHz),
            channelType = resolveChannelType(stream.channelsNumber)
        ),
        direction = resolveStreamDirection(stream.direction)
    )
}

private fun resolveCodecName(codecName: String): CodecName {
    return if (codecName.isNotEmpty()) {
        CodecName(codecName)

    } else {
        UNDEFINED_MEDIA_CODEC
    }
}

private fun resolveClockRate(clockRateHz: Int): ClockRate {
    return if (clockRateHz >= 0) {
        return ClockRate(clockRateHz)

    } else {
        UNDEFINED_CLOCK_RATE
    }
}
private fun resolveChannelType(channelsNumber: Int): ChannelType {
    return when (channelsNumber) {
        0 -> CHANNEL_TYPE_VIDEO
        1 -> CHANNEL_TYPE_AUDIO_MONO
        2 -> CHANNEL_TYPE_AUDIO_STEREO
        else -> UNDEFINED_CHANNEL_TYPE
    }
}

private fun resolveStreamDirection(direction: LinphoneCallStreamDirection): StreamDirection {
    return StreamDirection.valueOf(direction.name)
}
