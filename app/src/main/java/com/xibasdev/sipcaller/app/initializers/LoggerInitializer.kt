package com.xibasdev.sipcaller.app.initializers

import com.elvishew.xlog.XLog
import javax.inject.Inject
import javax.inject.Named

class LoggerInitializer @Inject constructor(
    @Named("LogLevel") private val logLevel: Int
) : LoggerInitializerApi {

    private var isInitialized: Boolean = false

    override fun initializeLogger() {
        if (!isInitialized) {
            isInitialized = true
            XLog.init(logLevel)
        }
    }
}
