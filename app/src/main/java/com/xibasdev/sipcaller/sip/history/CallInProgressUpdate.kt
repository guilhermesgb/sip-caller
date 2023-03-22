package com.xibasdev.sipcaller.sip.history

import com.xibasdev.sipcaller.sip.calling.streams.CallStreams

sealed interface CallInProgressUpdate : CallHistoryUpdate {

    val streams: CallStreams
}
