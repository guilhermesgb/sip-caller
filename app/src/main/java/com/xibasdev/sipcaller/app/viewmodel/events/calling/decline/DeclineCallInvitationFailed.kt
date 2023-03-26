package com.xibasdev.sipcaller.app.viewmodel.events.calling.decline

import com.xibasdev.sipcaller.app.viewmodel.common.OperationFailed

data class DeclineCallInvitationFailed(
    override val error: Throwable
) : DeclineCallInvitationEvent, OperationFailed
