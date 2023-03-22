package com.xibasdev.sipcaller.sip.calling.streams.codec

/**
 * Snapshot of the state of the media codec currently in use by an arbitrary stream. If the stream
 *   is not currently using any media codec, then it merely contains information signaling that the
 *   stream has an undefined codec, by means of the [UNDEFINED_MEDIA_CODEC], [UNDEFINED_CLOCK_RATE]
 *   and [UNDEFINED_CHANNEL_TYPE] values.
 */
data class MediaCodec(
    val codecName: CodecName = UNDEFINED_MEDIA_CODEC,
    val clockRateHz: ClockRate = UNDEFINED_CLOCK_RATE,
    val channelType: ChannelType = UNDEFINED_CHANNEL_TYPE
)
