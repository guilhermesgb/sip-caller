package com.xibasdev.sipcaller.app.viewmodel.common

import androidx.lifecycle.ViewModel
import com.xibasdev.sipcaller.app.viewmodel.profile.events.ViewModelEvent
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit

open class BaseViewModel : ViewModel() {

    private val disposables = CompositeDisposable()

    private val events = BehaviorSubject.create<ViewModelEvent>()

    internal fun <T : Any> Observable<T>.continuousDebouncer(
        onCompleteEvent: ViewModelEvent,
        onErrorEventProvider: (Throwable) -> ViewModelEvent,
        completableOperationProvider: (T) -> Completable
    ) {

        debounce(500, TimeUnit.MILLISECONDS)
            .switchMapCompletable {

                completableOperationProvider(it)
            }
            .continuouslyPropagateResultAsEvent(onCompleteEvent) { error ->

                onErrorEventProvider(error)
            }
    }

    internal fun Completable.continuouslyPropagateResultAsEvent(
        onCompleteEvent: ViewModelEvent,
        onErrorEventProvider: (Throwable) -> ViewModelEvent
    ) {

        doOnComplete {
            events.onNext(onCompleteEvent)
        }.doOnError { error ->

            events.onNext(onErrorEventProvider(error))
        }.onErrorResumeWith {
            continuouslyPropagateResultAsEvent(onCompleteEvent, onErrorEventProvider)
        }.subscribe()
            .addTo(disposables)
    }

    internal fun Completable.propagateResultAsEvent(
        onCompleteEvent: ViewModelEvent,
        onErrorEventProvider: (Throwable) -> ViewModelEvent
    ) = subscribeBy(
        onComplete = {
            events.onNext(onCompleteEvent)
        },
        onError = { error ->

            events.onNext(onErrorEventProvider(error))
        }
    ).addTo(disposables)

    internal fun propagateResultImmediately(event: ViewModelEvent) {
        events.onNext(event)
    }

    override fun onCleared() {
        disposables.dispose()
        super.onCleared()
    }
}
