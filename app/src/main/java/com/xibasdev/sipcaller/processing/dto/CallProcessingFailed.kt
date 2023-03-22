package com.xibasdev.sipcaller.processing.dto

/**
 * Representation of a call processing result that signals that an error has happened while trying
 *   to either start or stop call processing or that processing has been halted due to an error.
 */
data class CallProcessingFailed(val error: Throwable) : CallProcessing
