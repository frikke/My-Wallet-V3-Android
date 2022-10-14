package com.blockchain.core.connectivity

import io.reactivex.rxjava3.annotations.NonNull
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.PublishSubject.create

interface SSLPinningObservable {
    fun observeOn(scheduler: @NonNull Scheduler?): Observable<ConnectionEvent>
}

interface SSLPinningEmitter {
    fun emit()
}

class SSLPinningSubject : SSLPinningObservable, SSLPinningEmitter {
    private val subject: PublishSubject<ConnectionEvent> by lazy { create() }

    override fun observeOn(scheduler: @NonNull Scheduler?): Observable<ConnectionEvent> {
        return subject
            .observeOn(scheduler, false, Observable.bufferSize())
            .distinct()
    }

    override fun emit() {
        subject.onNext(ConnectionEvent.PINNING_FAIL)
    }
}
