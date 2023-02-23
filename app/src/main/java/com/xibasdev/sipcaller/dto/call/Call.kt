package com.xibasdev.sipcaller.dto.call

import com.xibasdev.sipcaller.dto.call.CallDirection.OUTGOING
import com.xibasdev.sipcaller.dto.call.CallStage.INVITATION
import com.xibasdev.sipcaller.dto.call.CallStatus.RINGING
import com.xibasdev.sipcaller.dto.call.features.CallFeatures
import com.xibasdev.sipcaller.dto.call.parties.CallParties
import com.xibasdev.sipcaller.dto.call.streams.CallStreams

/**
 * Snapshot of the complete state of an arbitrary call.
 *
 * Calls can be represented by several [stage]s, changing over time.
 *
 * An arbitrary call is in the [com.xibasdev.sipcaller.dto.CallStage.INVITATION] stage while it
 *   hasn't been accepted by the callee (the local party in the case of incoming calls or the remote
 *   party in case of outgoing calls) or if either caller decides to cancel, callee decides to
 *   decline, callee misses the invitation or a remote callee accepts it elsewhere.
 *
 * An arbitrary call transitions to [com.xibasdev.sipcaller.dto.CallStage.SESSION] stage when it is
 *   accepted by the callee (the local party in the case of incoming calls or the remote party in
 *   case of outgoing calls). If the call session is later terminated by either local or remote
 *   party or if some error happens while the session was still in progress, the call remains in the
 *   session stage.
 *
 * Information about whether the call is outgoing or incoming is represented by the [direction]
 * property, of type [CallDirection] - it never changes for a call session/invitation.
 *
 * Further details on the status of the call is represented by the [status] property, of type
 * [CallStatus] - it changes in tandem with changes to the [stage] property:
 *
 * [com.xibasdev.sipcaller.dto.CallStage.INVITATION] stage can have these status:
 * - [com.xibasdev.sipcaller.dto.CallStatus.RINGING]
 * - [com.xibasdev.sipcaller.dto.CallStatus.CANCELED] (terminal)
 * - [com.xibasdev.sipcaller.dto.CallStatus.MISSED] (terminal)
 * - [com.xibasdev.sipcaller.dto.CallStatus.ACCEPTED_ELSEWHERE] (terminal)
 * - [com.xibasdev.sipcaller.dto.CallStatus.DECLINED] (terminal)
 *
 * [com.xibasdev.sipcaller.dto.CallStage.SESSION] stage can have these status:
 * - [com.xibasdev.sipcaller.dto.CallStatus.ACCEPTED]
 * - [com.xibasdev.sipcaller.dto.CallStatus.FINISHED_BY_LOCAL_PARTY] (terminal)
 * - [com.xibasdev.sipcaller.dto.CallStatus.FINISHED_BY_REMOTE_PARTY] (terminal)
 * - [com.xibasdev.sipcaller.dto.CallStatus.FINISHED_DUE_TO_ERROR] (terminal)
 *
 * The [callId] property contains a unique identifier for the call session/invitation, which is
 *   useful for doing many operations targeting a specific call, possible through certain APIs.
 *   TODO List the APIs here for which [callId] can be used here.
 *
 * The [durationMs] property contains the current total duration of the call session, in
 *   milliseconds. If the call never leaves the invitation stage, the duration stays at zero.
 *
 * Any given call session or invitation may be comprised of zero or one audio and zero or one video
 *   streams, whose state can change over time. Call invitations may exchange media in advance, what
 *   is known as "early media". In [streams], it is contained information on which audio and/or
 *   video codec is currently in use by the call's respective audio/video streams. In a nutshell,
 *   you can easily determine whether a given stream is currently active and if so, if it is active
 *   in a unidirectional or bidirectional fashion by checking:
 *
 * - [streams] followed by either [CallStreams.audio] or [CallStreams.video], followed up by
 *   [com.xibasdev.sipcaller.dto.streams.MediaStream.direction], which can be one of the following:
 *
 *   - [com.xibasdev.sipcaller.dto.streams.StreamDirection.DISABLED]: indicates that there's
 *       currently no data being exchanged in the corresponding media stream;
 *   - [com.xibasdev.sipcaller.dto.streams.StreamDirection.ENABLED_RECEIVE_ONLY]: indicates that
 *       currently data is ONLY being received from the remote party in the media stream;
 *   - [com.xibasdev.sipcaller.dto.streams.StreamDirection.ENABLED_SEND_ONLY]: indicates that
 *       currently data is ONLY being sent to the remote party in the corresponding media stream;
 *   - [com.xibasdev.sipcaller.dto.streams.StreamDirection.ENABLED_SEND_RECEIVE]: indicates that
 *       currently data is being exchanged bidirectionally to and from the remote party in the
 *       corresponding media stream.
 *
 * Any given call invitation or session will contain exclusively two involved parties: the local
 *   party, which always refers to an address through which the local device communicates, and the
 *   remote party, which always refers to the address where a remote device is located.
 *
 * These two statements are true regardless of the call's direction:
 *   If [OUTGOING], the local party is your device, which is placing the outgoing call and the
 *     remote party is the target of your outgoing call (for them, that's an incoming call).
 *   Otherwise, when it is [com.xibasdev.sipcaller.dto.CallDirection.INCOMING], then the local party
 *     is still your device, which is now receiving an incoming call and the remote party is the
 *     caller (for them, that's an outgoing call).
 *
 * In other words, [direction], as well as local and remote [parties] are always relative to the
 *   local context of the running app, never relative to the remote side of the communication.
 *
 * The following extension methods may be used for determining which party is the callee or the
 *   caller in any given call session or invitation:
 *
 *   - [com.xibasdev.sipcaller.dto.parties.isCaller]
 *   - [com.xibasdev.sipcaller.dto.parties.isCallee]
 *
 * Further information about [features], [parties] and [streams] can be found in their respective
 *   documentation.
 */
data class Call(
    val callId: CallId,
    val direction: CallDirection = OUTGOING,
    val durationMs: Long = 0L,
    val features: CallFeatures = CallFeatures(),
    val parties: CallParties = CallParties(),
    val stage: CallStage = INVITATION,
    val status: CallStatus = RINGING,
    val streams: CallStreams = CallStreams()
)
