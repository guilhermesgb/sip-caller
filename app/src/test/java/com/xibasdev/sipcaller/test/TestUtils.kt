package com.xibasdev.sipcaller.test

import com.xibasdev.sipcaller.test.Completable.afterDelay
import com.xibasdev.sipcaller.test.Observable.afterDelay
import com.xibasdev.sipcaller.test.Single.afterDelay
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.observers.TestObserver
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.schedulers.TestScheduler
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS


object Observable {

    fun <T : Any> Observable<T>.prepareInForegroundAndWaitUpToTimeout(
        timeoutDuration: Long = 2,
        timeoutUnit: TimeUnit = SECONDS
    ): TestObserver<T> {

        return prepareInBackgroundAndWaitUpToTimeout(TEST_SCHEDULER, timeoutDuration, timeoutUnit)
    }

    fun <T : Any> Observable<T>.prepareInBackgroundAndWaitUpToTimeout(
        scheduler: Scheduler = Schedulers.io(),
        timeoutDuration: Long = 2,
        timeoutUnit: TimeUnit = SECONDS
    ): TestObserver<T> {

        return prepareInBackground(scheduler).also { observer ->

            if (scheduler is TestScheduler) {
                observer.simulateWaitUpToTimeout(timeoutDuration, timeoutUnit)

            } else {
                observer.waitUpToTimeout(timeoutDuration, timeoutUnit)
            }
        }
    }

    fun <T : Any> Observable<T>.prepareInForeground(): TestObserver<T> {
        return prepareInBackground(TEST_SCHEDULER)
    }

    fun <T : Any> Observable<T>.prepareInBackground(
        scheduler: Scheduler = Schedulers.io()
    ): TestObserver<T> {

        return subscribeOn(scheduler).prepare()
    }

    private fun <T : Any> Observable<T>.prepare(): TestObserver<T> {
        return test()
    }

    fun <T : Any> Observable<T>.andThenAfterDelay(
        observable: Observable<T>,
        delayDuration: Long = 50,
        delayUnit: TimeUnit = MILLISECONDS
    ): Observable<T> {
        return ignoreElements().andThen(observable.afterDelay(delayDuration, delayUnit))
    }

    fun <T : Any> Observable<T>.andThenAfterDelay(
        completable: Completable,
        delayDuration: Long = 50,
        delayUnit: TimeUnit = MILLISECONDS
    ): Completable {
        return ignoreElements().andThen(completable.afterDelay(delayDuration, delayUnit))
    }

    fun <T: Any> Observable<T>.andThenAfterDelay(
        single: Single<T>,
        delayDuration: Long = 50,
        delayUnit: TimeUnit = MILLISECONDS
    ): Single<T> {
        return ignoreElements().andThen(single.afterDelay(delayDuration, delayUnit))
    }

    fun <T : Any> Observable<T>.afterDelay(
        delayDuration: Long = 50,
        delayUnit: TimeUnit = MILLISECONDS
    ): Observable<T> {
        return delaySubscription(delayDuration, delayUnit)
    }
}

object Completable {

    fun Completable.prepareInForegroundAndWaitUpToTimeout(
        timeoutDuration: Long = 2,
        timeoutUnit: TimeUnit = SECONDS
    ): TestObserver<Void> {

        return prepareInBackgroundAndWaitUpToTimeout(TEST_SCHEDULER, timeoutDuration, timeoutUnit)
    }

    fun Completable.prepareInBackgroundAndWaitUpToTimeout(
        scheduler: Scheduler = Schedulers.io(),
        timeoutDuration: Long = 2,
        timeoutUnit: TimeUnit = SECONDS
    ): TestObserver<Void> {

        return prepareInBackground(scheduler).also { observer ->

            if (scheduler is TestScheduler) {
                observer.simulateWaitUpToTimeout(timeoutDuration, timeoutUnit)

            } else {
                observer.waitUpToTimeout(timeoutDuration, timeoutUnit)
            }
        }
    }

    fun Completable.prepareInForeground(): TestObserver<Void> {
        return prepareInBackground(TEST_SCHEDULER)
    }

    fun Completable.prepareInBackground(
        scheduler: Scheduler = Schedulers.io()
    ): TestObserver<Void> {

        return subscribeOn(scheduler).prepare()
    }

    private fun Completable.prepare(): TestObserver<Void> {
        return test()
    }

    fun <T : Any> Completable.andThenAfterDelay(
        observable: Observable<T>,
        delayDuration: Long = 50,
        delayUnit: TimeUnit = MILLISECONDS
    ): Observable<T> {
        return andThen(observable.afterDelay(delayDuration, delayUnit))
    }

    fun Completable.andThenAfterDelay(
        completable: Completable,
        delayDuration: Long = 50,
        delayUnit: TimeUnit = MILLISECONDS
    ): Completable {
        return andThen(completable.afterDelay(delayDuration, delayUnit))
    }

    fun <T: Any> Completable.andThenAfterDelay(
        single: Single<T>,
        delayDuration: Long = 50,
        delayUnit: TimeUnit = MILLISECONDS
    ): Single<T> {
        return andThen(single.afterDelay(delayDuration, delayUnit))
    }

    fun Completable.simulateAfterDelay(
        delayDuration: Long = 50,
        delayUnit: TimeUnit = MILLISECONDS
    ): Completable {
        return delaySubscription(delayDuration, delayUnit, TEST_SCHEDULER)
    }

    fun Completable.afterDelay(
        delayDuration: Long = 50,
        delayUnit: TimeUnit = MILLISECONDS
    ): Completable {
        return delaySubscription(delayDuration, delayUnit)
    }
}

object Single {

    fun <T : Any> Single<T>.prepareInBackgroundAndWaitUpToTimeout(
        scheduler: Scheduler = Schedulers.io(),
        timeoutDuration: Long = 2,
        timeoutUnit: TimeUnit = SECONDS
    ): TestObserver<T> {

        return prepareInBackground(scheduler).also { observer ->

            if (scheduler is TestScheduler) {
                observer.simulateWaitUpToTimeout(timeoutDuration, timeoutUnit)

            } else {
                observer.waitUpToTimeout(timeoutDuration, timeoutUnit)
            }
        }
    }

    fun <T : Any> Single<T>.prepareInBackground(
        scheduler: Scheduler = Schedulers.io()
    ): TestObserver<T> {

        return subscribeOn(scheduler).prepare()
    }

    private fun <T : Any> Single<T>.prepare(): TestObserver<T> {
        return test()
    }

    fun <T: Any> Single<T>.andThenAfterDelay(
        observable: Observable<T>,
        delayDuration: Long = 50,
        delayUnit: TimeUnit = MILLISECONDS
    ): Observable<T> {
        return ignoreElement().andThen(observable.afterDelay(delayDuration, delayUnit))
    }

    fun <T: Any> Single<T>.andThenAfterDelay(
        completable: Completable,
        delayDuration: Long = 50,
        delayUnit: TimeUnit = MILLISECONDS
    ): Completable {
        return ignoreElement().andThen(completable.afterDelay(delayDuration, delayUnit))
    }

    fun <T: Any> Single<T>.andThenAfterDelay(
        single: Single<T>,
        delayDuration: Long = 50,
        delayUnit: TimeUnit = MILLISECONDS
    ): Single<T> {
        return ignoreElement().andThen(single.afterDelay(delayDuration, delayUnit))
    }

    fun <T : Any> Single<T>.afterDelay(
        delayDuration: Long = 50,
        delayUnit: TimeUnit = MILLISECONDS
    ): Single<T> {
        return delaySubscription(delayDuration, delayUnit)
    }
}


fun <T : Any> TestObserver<T>.simulateWaitUpToTimeout(
    timeoutDuration: Long = 2,
    timeoutUnit: TimeUnit = SECONDS
) {

    TEST_SCHEDULER.advanceTimeBy(timeoutDuration, timeoutUnit)
    TEST_SCHEDULER.triggerActions()
}


fun <T : Any> TestObserver<T>.waitUpToTimeout(
    timeoutDuration: Long = 2,
    timeoutUnit: TimeUnit = SECONDS
) {

    await(timeoutDuration, timeoutUnit)
}


val TEST_SCHEDULER = TestScheduler()
