package com.blockchain.utils

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

// converts a List<Single<Items>> -> Single<List<Items>>
fun <T> List<Single<T>>.zipSingles(): Single<List<T>> {
    if (this.isEmpty()) return Single.just(emptyList())
    return Single.zip(this) {
        @Suppress("UNCHECKED_CAST")
        return@zip (it as Array<T>).toList()
    }
}

// converts a List<Observable<Items>> -> Observable<List<Items>>
fun <T> List<Observable<T>>.zipObservables(): Observable<List<T>> {
    if (this.isEmpty()) return Observable.just(emptyList())
    return Observable.zip(this) {
        @Suppress("UNCHECKED_CAST")
        return@zip (it as Array<T>).toList()
    }
}
