package com.xibasdev.sipcaller.processing.dto

/**
 * Representation of a call processing result observed via
 *   [com.xibasdev.sipcaller.processing.CallProcessor.observeProcessing].
 *
 * Call processing refers to the process of starting the [com.xibasdev.sipcaller.sip.SipEngineApi]
 *   in the background with [com.xibasdev.sipcaller.sip.SipEngineApi.startEngine] and continuously
 *   iterating the engine's recurring steps with
 *   [com.xibasdev.sipcaller.sip.SipEngineApi.processEngineSteps].
 */
sealed interface CallProcessing
