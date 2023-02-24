package com.xibasdev.sipcaller.sip.history

sealed interface CallFailedUpdate : CallHistoryUpdate {
    val errorReason: String
}
