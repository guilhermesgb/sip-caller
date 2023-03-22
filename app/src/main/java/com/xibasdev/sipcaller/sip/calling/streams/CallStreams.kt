package com.xibasdev.sipcaller.sip.calling.streams

/**
 * Snapshot of the state of both audio and video streams of an arbitrary call. If an arbitrary call
 *   is in a terminal state (as indicated by
 *   [com.xibasdev.sipcaller.sip.calling.CallStatus.isTerminal]), then both streams will be
 *   disabled, i.e. their direction will be set to
 *   [com.xibasdev.sipcaller.sip.calling.streams.StreamDirection.DISABLED], while the last codecs
 *   that were in use while the call session or invitation was ongoing will remain set in the
 *   streams.
 */
data class CallStreams(
    val audio: MediaStream = MediaStream(),
    val video: MediaStream = MediaStream()
)
