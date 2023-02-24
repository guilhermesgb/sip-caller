package com.xibasdev.sipcaller.sip

import com.xibasdev.sipcaller.sip.processing.ProcessingEngineApi
import com.xibasdev.sipcaller.sip.history.CallHistoryObserverApi


/**
 * Public API that serves as a contract between components interested in operating some underlying
 *   SIP library for processing call invitations (and ongoing call sessions) and the component that
 *   implements such processing using a SIP library as its backbone (i.e. the liblinphone-based
 *   [com.xibasdev.sipcaller.sip.linphone.LinphoneSipEngine]).
 */
interface SipEngineApi : ProcessingEngineApi, CallHistoryObserverApi
