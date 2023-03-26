package com.xibasdev.sipcaller.app.viewmodel.common

interface OperationFailed : ViewModelEvent {
    val error: Throwable
}
