package com.xibasdev.sipcaller.sip.calling.details

import com.xibasdev.sipcaller.sip.calling.Call

data class CallInvitationUpdate(
    val call: Call
) : CallUpdate
