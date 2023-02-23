package com.xibasdev.sipcaller.dto.call.parties

import com.xibasdev.sipcaller.dto.call.CallDirection.INCOMING
import com.xibasdev.sipcaller.dto.call.CallDirection.OUTGOING
import com.xibasdev.sipcaller.dto.call.Call
import com.xibasdev.sipcaller.dto.call.parties.address.PartyAddress
import com.xibasdev.sipcaller.dto.call.parties.address.PartyIpAddress

/**
 * Representation of information pertaining to one of the two parties involved in a call session or
 *   invitation, such as its display name, username and address.
 */
data class CallParty(
    val displayName: PartyDisplayName = EMPTY_DISPLAY_NAME,
    val username: PartyUsername = UNKNOWN_USERNAME,
    val address: PartyAddress = PartyIpAddress()
)

context(Call)
fun CallParty.isCaller(): Boolean {
    return (direction == OUTGOING && parties.local == this)
            || (direction == INCOMING && parties.remote == this)
}

context(Call)
fun CallParty.isCallee(): Boolean {
    return (direction == OUTGOING && parties.remote == this)
            || (direction == INCOMING && parties.local == this)
}
