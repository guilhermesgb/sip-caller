package com.xibasdev.sipcaller.sip.linphone.context

interface FakeLinphoneContextApi {

    fun failSynchronouslyOnLinphoneCoreStart()

    fun failAsynchronouslyOnLinphoneCoreStart()

    fun failSynchronouslyOnLinphoneCoreIterate()

    fun simulateLinphoneCoreStop()

    fun simulateIncomingCallInvitationArrived()

    fun simulateDeclineIncomingCallInvitation(callId: String)

    fun simulateAcceptIncomingCallInvitation(callId: String)

    fun simulateMissIncomingCallInvitation(callId: String)

    fun simulateIncomingCallInvitationAcceptedElsewhere(callId: String)

    fun simulateIncomingCallCanceledByCaller(callId: String)

    fun simulateOutgoingCallInvitationSent()
}
