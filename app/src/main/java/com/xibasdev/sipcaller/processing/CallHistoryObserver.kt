package com.xibasdev.sipcaller.processing

import com.xibasdev.sipcaller.app.utils.Indexed
import com.xibasdev.sipcaller.sip.SipEngineApi
import com.xibasdev.sipcaller.sip.history.CallHistoryUpdate
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class CallHistoryObserver @Inject constructor(private val sipEngine: SipEngineApi) {

    fun observeCallHistory(offset: OffsetDateTime): Observable<List<Indexed<CallHistoryUpdate>>> {
        return Observable
            .zip(
                Observable.interval(500, TimeUnit.MILLISECONDS),
                sipEngine.observeCallHistory(offset)
                    .flatMap { updates ->

                        Observable.fromIterable(updates)
                    }
            ) { index, update ->

                Indexed(index, update)
            }
            .scan(listOf<Indexed<CallHistoryUpdate>>()) { previous, next ->

                val filteredPrevious = previous
                    .drop(Integer.max(0, previous.size - 9))
                    .toTypedArray()

                listOf(*filteredPrevious, next)
            }
            .map { updates ->

                updates.reversed()
            }
            .observeOn(AndroidSchedulers.mainThread())
    }
}
