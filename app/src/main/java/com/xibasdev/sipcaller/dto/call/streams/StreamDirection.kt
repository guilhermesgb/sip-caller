package com.xibasdev.sipcaller.dto.call.streams

/**
 * Snapshot of the direction state of a media stream. Media stream state can change over time. This
 *   enum encodes the following information: if the media stream is enabled or not and if enabled,
 *   which directions are currently enabled (media streams can be unidirectional outgoing/incoming
 *   streams or bidirectional streams (with incoming and outgoing data exchange simultaneously).
 */
enum class StreamDirection {
    /**
     * The media stream is disabled in both directions.
     */
    DISABLED,

    /**
     * The media stream is enabled, unidirectional for incoming data
     */
    ENABLED_RECEIVE_ONLY,

    /**
     * The media stream is enabled, unidirectional for outgoing data
     */
    ENABLED_SEND_ONLY,

    /**
     * The media stream is enabled, bidirectional (incoming and outgoing data)
     */
    ENABLED_SEND_RECEIVE
}
