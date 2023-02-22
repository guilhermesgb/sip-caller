package com.xibasdev.sipcaller.dto.parties

/**
 * This value encodes the display name of an arbitrary party involved in an arbitrary call. It is
 *   optional and so it may be observed as [EMPTY_DISPLAY_NAME] for a given call party.
 */
@JvmInline
value class PartyDisplayName(val value: String)

val EMPTY_DISPLAY_NAME = PartyDisplayName("")
