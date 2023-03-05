package com.xibasdev.sipcaller.sip

interface FakeSipEngineApi {

    fun simulateFailureWhileProcessingEngineSteps()

    fun revertFailureSimulationWhileProcessingEngineSteps()
}
