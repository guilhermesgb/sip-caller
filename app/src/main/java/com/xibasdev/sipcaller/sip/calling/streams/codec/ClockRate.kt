package com.xibasdev.sipcaller.sip.calling.streams.codec

/**
 * This value encodes the clock rate, in hertz, of an arbitrary codec.
 *
 * If the codec in use by some stream is undefined, then this value equals [UNDEFINED_CLOCK_RATE].
 */
@JvmInline
value class ClockRate internal constructor(val value: Int)

val UNDEFINED_CLOCK_RATE = ClockRate(-1)
