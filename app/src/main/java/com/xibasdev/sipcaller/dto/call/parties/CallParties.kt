package com.xibasdev.sipcaller.dto.call.parties

/**
 * Representation of the parties involved in a call session or invitation.
 *
 * If the call is [com.xibasdev.sipcaller.dto.CallDirection.OUTGOING], then the local party is the
 *   caller, the user of your app who is currently placing an outgoing call. That being the case,
 *   the remote party refers to the callee, the target of the outgoing call.
 *
 * Conversely, if the call is [com.xibasdev.sipcaller.dto.CallDirection.INCOMING], then the local
 *   party refers to the callee, meaning the user of your app is receiving a call from a remote
 *   party. The being the case, then the remote party refers to the caller, the origin of the
 *   incoming call.
 */
data class CallParties(
    val local: CallParty = CallParty(),
    val remote: CallParty = CallParty()
)
