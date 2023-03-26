package com.xibasdev.sipcaller.app.viewmodel.common

import androidx.lifecycle.ViewModel
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

    fun observeEvents(): Observable<ViewModelEvent> {
        return events
    }

    internal fun <T : Any> Observable<T>.debounceAndContinuouslyPropagateResultAsEvent(
        onRunningEvent: ViewModelEvent,
        onCompleteEvent: ViewModelEvent,
        onErrorEventProvider: (Throwable) -> ViewModelEvent,
        completableOperationProvider: (T) -> Completable
    ) {

        debounce(500, TimeUnit.MILLISECONDS)
            .switchMapCompletable {

                completableOperationProvider(it)
                    .doOnSubscribe { events.onNext(onRunningEvent) }
                    .doOnComplete { events.onNext(onCompleteEvent) }
            }
            .doOnError { error ->

                events.onNext(onErrorEventProvider(error))
            }
            .onErrorResumeWith {
                debounceAndContinuouslyPropagateResultAsEvent(
                    onRunningEvent, onCompleteEvent, onErrorEventProvider,
                    completableOperationProvider
                )
            }
            .subscribe()
            .addTo(disposables)
    }

    internal fun Completable.continuouslyPropagateResultAsEvent(
        onRunningEvent: ViewModelEvent,
        onCompleteEvent: ViewModelEvent,
        onErrorEventProvider: (Throwable) -> ViewModelEvent
    ) {

        doOnSubscribe {
            events.onNext(onRunningEvent)
        }.doOnComplete {
            events.onNext(onCompleteEvent)
        }.doOnError { error ->

            events.onNext(onErrorEventProvider(error))
        }.onErrorResumeWith {
            continuouslyPropagateResultAsEvent(
                onRunningEvent, onCompleteEvent, onErrorEventProvider
            )
        }.subscribe()
            .addTo(disposables)
    }

    internal fun Completable.propagateResultAsEvent(
        onRunningEvent: ViewModelEvent,
        onCompleteEvent: ViewModelEvent,
        onErrorEventProvider: (Throwable) -> ViewModelEvent
    ) = doOnSubscribe {
        events.onNext(onRunningEvent)
    }.subscribeBy(
        onComplete = { events.onNext(onCompleteEvent) },
        onError = { error ->

            events.onNext(onErrorEventProvider(error))
        }
    ).addTo(disposables)

    override fun onCleared() {
        disposables.dispose()
        super.onCleared()
    }
}
