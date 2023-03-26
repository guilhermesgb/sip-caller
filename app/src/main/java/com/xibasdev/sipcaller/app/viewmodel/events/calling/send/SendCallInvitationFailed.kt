package com.xibasdev.sipcaller.app.viewmodel.events.calling.send

import com.xibasdev.sipcaller.app.viewmodel.common.OperationFailed

data class SendCallInvitationFailed(
    override val error: Throwable
) : SendCallInvitationEvent, OperationFailed
