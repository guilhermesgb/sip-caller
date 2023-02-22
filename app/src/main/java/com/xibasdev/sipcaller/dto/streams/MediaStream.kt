package com.xibasdev.sipcaller.dto.streams

import com.xibasdev.sipcaller.dto.streams.StreamDirection.DISABLED
import com.xibasdev.sipcaller.dto.streams.codec.MediaCodec

/**
 * Snapshot of the entire state of an arbitrary media stream, including which codec is currently
 *   in use, the nature of the stream in terms of the data exchange direction (disabled or enabled
 *   in unidirectional or bidirectional fashion) as well up-to-date media statistics information.
 */
data class MediaStream(
    val codec: MediaCodec = MediaCodec(),
    val direction: StreamDirection = DISABLED,
    val statistics: StreamStatistics = StreamStatistics()
)
