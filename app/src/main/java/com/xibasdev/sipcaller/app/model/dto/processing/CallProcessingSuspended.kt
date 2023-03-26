package com.xibasdev.sipcaller.app.model.dto.processing

/**
 * Representation of a call processing result that signals that processing has been temporarily
 *     suspended due to some temporary condition. Known conditions that may lead to this:
 *
 *   - When there's no active network connection;
 *   - When the app's background processing quota has been exceeded.
 *
 *   The processing should seamlessly resume (back to [CallProcessingStarted]) as soon as the
 *     condition that led to suspension is no longer present.
 */
object CallProcessingSuspended : CallProcessing
