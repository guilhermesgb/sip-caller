package com.xibasdev.sipcaller.app.workers

import androidx.work.WorkerFactory

abstract class CustomWorkerFactory : WorkerFactory() {

    abstract fun getWorkerClassName(): String
}
