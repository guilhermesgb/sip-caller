package com.xibasdev.sipcaller.dto.call.parties

/**
 * This value encodes the username of an arbitrary party involved in an arbitrary call. This is an
 *   optional value, and so it may be observed as [UNKNOWN_USERNAME] for a given call party.
 */
@JvmInline
value class PartyUsername(val value: String)

val UNKNOWN_USERNAME = PartyUsername("unknown")
