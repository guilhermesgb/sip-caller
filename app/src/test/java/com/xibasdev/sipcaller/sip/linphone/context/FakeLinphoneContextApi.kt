package com.xibasdev.sipcaller.sip.linphone.context

interface FakeLinphoneContextApi {

    fun failSynchronouslyOnLinphoneCoreStart()

    fun failAsynchronouslyOnLinphoneCoreStart()

    fun failSynchronouslyOnLinphoneCoreIterate()

    fun failSynchronouslyOnAccountCreation()

    fun failAsynchronouslyOnAccountRegistration()

    fun failSynchronouslyOnAccountDeactivation()

    fun failAsynchronouslyOnAccountUnregistration()

    fun failSynchronouslyOnAccountDestruction()

    fun simulateLinphoneCoreStop()

    fun simulateIncomingCallInvitationArrived()

    fun simulateDeclineIncomingCallInvitation(callId: String)

    fun simulateAcceptIncomingCallInvitation(callId: String)

    fun simulateMissIncomingCallInvitation(callId: String)

    fun simulateIncomingCallInvitationAcceptedElsewhere(callId: String)

    fun simulateIncomingCallCanceledByCaller(callId: String)

    fun simulateIncomingCallCanceledByCallee(callId: String)

    fun simulateOutgoingCallInvitationSent()
}
