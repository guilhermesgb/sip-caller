package com.xibasdev.sipcaller.sip.registering.account

/**
 * This value encodes the display name of an arbitrary party involved in an arbitrary call. It is
 *   optional and so it may be observed as [EMPTY_DISPLAY_NAME] for a given call party.
 */
@JvmInline
value class AccountDisplayName(val value: String)

val EMPTY_DISPLAY_NAME = AccountDisplayName("")
