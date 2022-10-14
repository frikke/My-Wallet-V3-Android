@file:JvmName("RxSchedulingExtensions")

package com.blockchain.core.utils.schedulers

import com.blockchain.logging.Logger
import com.blockchain.rx.MainScheduler
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers

/**
 * Applies standard Schedulers to an [Observable], ie IO for subscription, Main Thread for
 * onNext/onComplete/onError
 */
fun <T> Observable<T>.applySchedulers(): Observable<T> =
    this.subscribeOn(Schedulers.io())
        .observeOn(MainScheduler.main())
        .doOnError(Logger::e)

/**
 * Applies standard Schedulers to a [Single], ie IO for subscription, Main Thread for
 * onNext/onComplete/onError
 */
fun <T> Single<T>.applySchedulers(): Single<T> =
    this.subscribeOn(Schedulers.io())
        .observeOn(MainScheduler.main())
        .doOnError(Logger::e)

/**
 * Applies standard Schedulers to a [Completable], ie IO for subscription,
 * Main Thread for onNext/onComplete/onError
 */
fun Completable.applySchedulers(): Completable =
    this.subscribeOn(Schedulers.io())
        .observeOn(MainScheduler.main())
        .doOnError(Logger::e)

/**
 * Applies standard Schedulers to an [Observable], ie IO for subscription, Main Thread for
 * onNext/onComplete/onError
 */
fun <T> Maybe<T>.applySchedulers(): Maybe<T> =
    this.subscribeOn(Schedulers.io())
        .observeOn(MainScheduler.main())
        .doOnError(Logger::e)
