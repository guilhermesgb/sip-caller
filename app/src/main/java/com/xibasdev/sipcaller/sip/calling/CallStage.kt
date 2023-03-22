package com.xibasdev.sipcaller.sip.calling

/**
 * Snapshot of the state of an arbitrary call's current stage. An arbitrary incoming/outgoing call
 *   naturally has its stages transition across these possible states.
 */
enum class CallStage {
    /**
     * The call is in the invitation stage while it still rings on the callee side, it is canceled
     *   by the caller, declined or missed by the callee, or accepted by another callee elsewhere.
     */
    INVITATION,

    /**
     * The call is in session stage while it is accepted by the callee, and it hasn't been yet
     *   terminated by either caller or callee or after being terminated either intentionally or due
     *   to some error that happened while the call session was ongoing.
     */
    SESSION
}
