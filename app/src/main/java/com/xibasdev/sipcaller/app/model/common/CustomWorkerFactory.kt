package com.xibasdev.sipcaller.app.model.common

import androidx.work.WorkerFactory

abstract class CustomWorkerFactory : WorkerFactory() {

    abstract fun getWorkerClassName(): String
}
