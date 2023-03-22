package com.xibasdev.sipcaller.sip.calling.streams.codec

/**
 * This value encodes the number of supported channels an arbitrary codec supports.
 *
 * Video codecs support zero channels, hence [CHANNEL_TYPE_VIDEO] equals 0.
 *
 * Audio codecs can support 'monaural sound' (one channel), so [CHANNEL_TYPE_AUDIO_MONO] equals 1.
 *
 * Conversely, audio codecs may support 'stereo sound' (two channels), so
 *   [CHANNEL_TYPE_AUDIO_STEREO] equals 2.
 *
 * If the codec in use by some stream is undefined, then this value equals [UNDEFINED_CHANNEL_TYPE].
 */
@JvmInline
value class ChannelType internal constructor(val value: Int)

val UNDEFINED_CHANNEL_TYPE = ChannelType(-1)
val CHANNEL_TYPE_VIDEO = ChannelType(0)
val CHANNEL_TYPE_AUDIO_MONO = ChannelType(1)
val CHANNEL_TYPE_AUDIO_STEREO = ChannelType(2)
