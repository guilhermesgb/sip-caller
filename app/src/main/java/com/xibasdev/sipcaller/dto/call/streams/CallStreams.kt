package com.xibasdev.sipcaller.dto.call.streams

/**
 * Snapshot of the state of both audio and video streams of an arbitrary call. If an arbitrary call
 *   is in a terminal state (as indicated by [com.xibasdev.sipcaller.dto.CallStatus.isTerminal]),
 *   then both streams will be disabled, i.e. their direction will be set to
 *   [com.xibasdev.sipcaller.dto.streams.StreamDirection.DISABLED], while the last codecs that were
 *   in use while the call session or invitation was ongoing will remain set in the streams.
 */
data class CallStreams(
    val audio: MediaStream = MediaStream(),
    val video: MediaStream = MediaStream()
)
