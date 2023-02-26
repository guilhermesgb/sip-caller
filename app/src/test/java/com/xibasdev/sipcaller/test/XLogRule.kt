package com.xibasdev.sipcaller.test

import com.elvishew.xlog.LogLevel.ALL
import com.elvishew.xlog.XLog
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class XLogRule : TestRule {

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                if (!xLogInitialized) {
                    xLogInitialized = true
                    XLog.init(ALL)
                }

                base.evaluate()
            }
        }
    }

    companion object {
        private var xLogInitialized = false
    }
}
