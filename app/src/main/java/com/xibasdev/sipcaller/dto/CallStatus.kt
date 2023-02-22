package com.xibasdev.sipcaller.dto

/**
 * Snapshot of the state of an arbitrary call's current status. An arbitrary incoming/outgoing call
 *   naturally has its status transition across these possible states.
 *
 * A convenience value exists for easily checking if a given call status represents the end of a
 *   call session or invitation: [isTerminal].
 */
enum class CallStatus(val isTerminal: Boolean) {
    /**
     * The call invitation is ringing on the callee side (local party for incoming calls or remote
     *   party for outgoing calls).
     */
    RINGING(false),

    /**
     * The call invitation was canceled by the caller (local party for outgoing calls or remote
     *   party for incoming calls).
     */
    CANCELED(true),

    /**
     * The call invitation was missed by the callee (local party for incoming calls or remote party
     *   for outgoing calls).
     */
    MISSED(true),

    /**
     * The call invitation was accepted by a remote callee (can only happen for incoming calls,
     *   where the local party is a callee that misses the incoming invitation due to it being
     *   accepted by another callee located elsewhere).
     */
    ACCEPTED_ELSEWHERE(true),

    /**
     * The call invitation was declined by the callee (local party for incoming calls or remote
     *   party for outgoing calls).
     */
    DECLINED(true),

    /**
     * The call invitation was accepted by the callee (local party for incoming calls or remote
     *   party for outgoing calls).
     */
    ACCEPTED(false),

    /**
     * The call session was terminated by the local party (callee for incoming calls or caller for
     *   outgoing calls).
     */
    FINISHED_BY_LOCAL_PARTY(true),

    /**
     * The call session was terminated by the remote party (callee for outgoing calls or caller for
     *   incoming calls).
     */
    FINISHED_BY_REMOTE_PARTY(true),

    /**
     * The call session was terminated due to an error happening while the session was ongoing.
     */
    FINISHED_DUE_TO_ERROR(true)
}
