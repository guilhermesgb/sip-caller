package com.xibasdev.sipcaller.dto.call.streams.codec

/**
 * This value encodes a name used for identifying an arbitrary audio or video codec.
 *
 * If the codec in use by some stream is undefined, then this value equals [UNDEFINED_MEDIA_CODEC].
 */
@JvmInline
value class CodecName internal constructor(val value: String)

val UNDEFINED_MEDIA_CODEC = CodecName("Undefined")
