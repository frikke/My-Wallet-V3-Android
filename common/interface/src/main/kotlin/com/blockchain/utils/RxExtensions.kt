package com.blockchain.utils

import com.blockchain.data.DataResource
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.rx3.await

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

// Transform a Single into a Flow. Optionally can transform the type of the data too
internal fun <T : Any, R> Single<T>.flatMapToFlow(mapper: (T) -> R, catcher: (Throwable) -> R = { throw it }) =
    flow {
        emit(mapper(this@flatMapToFlow.await()))
    }
        .catch { catcher(it) }

// Single -> Flow while keeping the type
fun <T : Any> Single<T>.asFlow() = flatMapToFlow(
    mapper = { it }
)

// Single -> Flow while wrapping the result in DataResource
fun <T : Any> Single<T>.toFlowDataResource() = flatMapToFlow<T, DataResource<T>>(
    mapper = { DataResource.Data(it) }
)
    .catch {
        emit(DataResource.Error(Exception(it)))
    }
