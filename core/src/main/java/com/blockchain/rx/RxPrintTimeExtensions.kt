package com.blockchain.rx

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

fun <T : Any> Single<T>.printTime(tag: String): Single<T> {
    var timer = 0L
    return this.doOnSubscribe {
        println("Start time for $tag")
        timer = System.currentTimeMillis()
    }.doFinally {
        println("Total time for $tag ${System.currentTimeMillis() - timer}")
    }
}

fun Completable.printTime(tag: String): Completable {
    var timer = 0L
    return this.doOnSubscribe {
        println("Start time for $tag")
        timer = System.currentTimeMillis()
    }.doFinally {
        println("Total time for $tag ${System.currentTimeMillis() - timer}")
    }
}

fun <T : Any> Observable<T>.printTime(tag: String): Observable<T> {
    var timer = 0L
    return this.doOnSubscribe {
        println("Start time for $tag")
        timer = System.currentTimeMillis()
    }.doFinally {
        println("Total time for $tag ${System.currentTimeMillis() - timer}")
    }
}
