package com.xibasdev.sipcaller.dto.call

/**
 * Enumeration of the possible call direction states: [OUTGOING] for calls being made by the local
 *   party and [INCOMING] for calls being received from some remote party.
 */
enum class CallDirection {
    /**
     * A call is outgoing when it has been started by the local party.
     */
    OUTGOING,

    /**
     * A call is incoming when it has been started by the remote party.
     */
    INCOMING
}
