package com.xibasdev.sipcaller.sip.history

import io.reactivex.rxjava3.core.Observable

interface CallHistoryObserverApi {

    /**
     * Observes call history as processed by the SIP Engine as a result of its underlying background
     *   processing. Call history is updated by the SIP Engine in those scenarios:
     *
     *   - when a new call invitation is fully processed by the SIP Engine (outgoing or incoming);
     *   - when a previously-notified call invitation is terminated (i.e. canceled or declined);
     *   - when a call session (derived from a previously-notified call invitation) is terminated.
     *
     * These call history update notifications are represented by the [CallHistoryUpdate] type.
     *
     * When subscribing for updates, the subscriber will receive the entire history since the SIP
     *     Engine first started its underlying background processing (including subsequent updates
     *     if it was stopped and restarted again).
     *
     *   It is up to the subscriber to filter out unneeded updates.
     */
    fun observeCallHistory(): Observable<List<CallHistoryUpdate>>
}
