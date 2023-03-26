package com.xibasdev.sipcaller.sip.calling.details

import com.xibasdev.sipcaller.sip.calling.Call

data class CallSessionUpdate(
    val call: Call
) : CallUpdate
