package com.blockchain.data

import com.blockchain.outcome.Outcome
import com.blockchain.utils.combineMore
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.rx3.asCoroutineDispatcher
import kotlinx.coroutines.rx3.asObservable
import kotlinx.coroutines.rx3.rxMaybe
import kotlinx.coroutines.rx3.rxSingle

/**
 * [Loading] : emitted exclusively when fetching from network, the next emitted Data or Error will be related to the network fetch and mean that Store is no longer Loading
 * [Data] : emitted when the fetcher completes successfully or when we get a Cached value
 * [Error] : emitted exclusively when fetching from network, when a Fetcher error has occurred
 */
sealed class DataResource<out T> {
    object Loading : DataResource<Nothing>()
    data class Data<out T>(val data: T) : DataResource<T>()
    data class Error(val error: Exception) : DataResource<Nothing>() {
        override fun equals(other: Any?): Boolean {
            return if (other is Error) {
                error.message == other.error.message
            } else {
                false
            }
        }

        override fun hashCode(): Int {
            return error.hashCode()
        }
    }
}

fun <T, R> DataResource<T>.map(transform: (T) -> R): DataResource<R> {
    return when (this) {
        DataResource.Loading -> DataResource.Loading
        is DataResource.Error -> DataResource.Error(error)
        is DataResource.Data -> DataResource.Data(transform(data))
    }
}

fun <T, R> DataResource<Iterable<T>>.mapList(mapper: (T) -> R): DataResource<List<R>> {
    return map {
        it.map { mapper(it) }
    }
}

fun <T> DataResource<Iterable<T>>.filter(transform: (T) -> Boolean): DataResource<List<T>> {
    return when (this) {
        DataResource.Loading -> DataResource.Loading
        is DataResource.Error -> DataResource.Error(error)
        is DataResource.Data -> DataResource.Data(this.data.filter { transform(it) })
    }
}

fun <T, R> DataResource<T>.flatMap(transform: (T) -> DataResource<R>): DataResource<R> {
    return when (this) {
        DataResource.Loading -> DataResource.Loading
        is DataResource.Error -> DataResource.Error(error)
        is DataResource.Data -> transform(data)
    }
}

fun <T> DataResource<T>.doOnLoading(f: () -> Unit): DataResource<T> {
    return also {
        if (this is DataResource.Loading) f()
    }
}

fun <T> DataResource<T>.doOnData(f: (T) -> Unit): DataResource<T> {
    return also {
        if (this is DataResource.Data) f(this.data)
    }
}

fun <T> Flow<DataResource<T>>.doOnData(f: suspend (T) -> Unit): Flow<DataResource<T>> {
    return map { dataResource ->
        dataResource.also {
            if (it is DataResource.Data) f(it.data)
        }
    }
}

fun <T> Flow<DataResource<T>>.doOnError(f: suspend (Exception) -> Unit): Flow<DataResource<T>> {
    return map { dataResource ->
        dataResource.also {
            if (it is DataResource.Error) f(it.error)
        }
    }
}

fun <T> DataResource<T>.doOnError(f: (Exception) -> Unit): DataResource<T> {
    return also {
        if (this is DataResource.Error) f(this.error)
    }
}

fun <T> Iterable<DataResource<T>>.anyLoading() = any { it is DataResource.Loading }
fun <T> Iterable<DataResource<T>>.anyError() = any { it is DataResource.Error }
fun <T> Iterable<DataResource<T>>.getFirstError() = (first { it is DataResource.Error } as DataResource.Error)

fun <T> Flow<DataResource<T>>.onErrorReturn(errorToData: (Exception) -> T): Flow<DataResource<T>> {
    return map { dataResource ->
        when (dataResource) {
            is DataResource.Error -> {
                DataResource.Data(errorToData(dataResource.error))
            }

            else -> dataResource
        }
    }
}

fun <T> Iterable<DataResource<T>>.merge(transform: (Iterable<T>) -> T): DataResource<T> {
    return if (none { it is DataResource.Data }) {
        if (anyLoading()) {
            DataResource.Loading
        } else {
            getFirstError()
        }
    } else {
        DataResource.Data(
            transform(
                filterIsInstance<DataResource.Data<T>>()
                    .map { it.data }
            )
        )
    }
}

fun <T1, T2, R> combineDataResourceFlows(
    flow1: Flow<DataResource<T1>>,
    flow2: Flow<DataResource<T2>>,
    transform: (T1, T2) -> R
): Flow<DataResource<R>> = combine(flow1, flow2) { t1, t2 ->
    combineDataResources(t1, t2, transform)
}

fun <T1, T2, T3, R> combineDataResourceFlows(
    flow1: Flow<DataResource<T1>>,
    flow2: Flow<DataResource<T2>>,
    flow3: Flow<DataResource<T3>>,
    transform: (T1, T2, T3) -> R
): Flow<DataResource<R>> = combine(flow1, flow2, flow3) { t1, t2, t3 ->
    combineDataResources(t1, t2, t3, transform)
}

fun <T1, T2, T3, T4, R> combineDataResourceFlows(
    flow1: Flow<DataResource<T1>>,
    flow2: Flow<DataResource<T2>>,
    flow3: Flow<DataResource<T3>>,
    flow4: Flow<DataResource<T4>>,
    transform: (T1, T2, T3, T4) -> R
): Flow<DataResource<R>> = combine(flow1, flow2, flow3, flow4) { t1, t2, t3, t4 ->
    combineDataResources(t1, t2, t3, t4, transform)
}

fun <T1, T2, T3, T4, T5, R> combineDataResourceFlows(
    flow1: Flow<DataResource<T1>>,
    flow2: Flow<DataResource<T2>>,
    flow3: Flow<DataResource<T3>>,
    flow4: Flow<DataResource<T4>>,
    flow5: Flow<DataResource<T5>>,
    transform: (T1, T2, T3, T4, T5) -> R
): Flow<DataResource<R>> = combine(flow1, flow2, flow3, flow4, flow5) { t1, t2, t3, t4, t5 ->
    combineDataResources(t1, t2, t3, t4, t5, transform)
}

fun <T1, T2, T3, T4, T5, T6, R> combineDataResourceFlows(
    flow1: Flow<DataResource<T1>>,
    flow2: Flow<DataResource<T2>>,
    flow3: Flow<DataResource<T3>>,
    flow4: Flow<DataResource<T4>>,
    flow5: Flow<DataResource<T5>>,
    flow6: Flow<DataResource<T6>>,
    transform: (T1, T2, T3, T4, T5, T6) -> R
): Flow<DataResource<R>> = combineMore(flow1, flow2, flow3, flow4, flow5, flow6) { t1, t2, t3, t4, t5, t6 ->
    combineDataResources(t1, t2, t3, t4, t5, t6, transform)
}

fun <T1, T2, T3, T4, T5, T6, T7, R> combineDataResourceFlows(
    flow1: Flow<DataResource<T1>>,
    flow2: Flow<DataResource<T2>>,
    flow3: Flow<DataResource<T3>>,
    flow4: Flow<DataResource<T4>>,
    flow5: Flow<DataResource<T5>>,
    flow6: Flow<DataResource<T6>>,
    flow7: Flow<DataResource<T7>>,
    transform: (T1, T2, T3, T4, T5, T6, T7) -> R
): Flow<DataResource<R>> = combineMore(flow1, flow2, flow3, flow4, flow5, flow6, flow7) { t1, t2, t3, t4, t5, t6, t7 ->
    combineDataResources(t1, t2, t3, t4, t5, t6, t7, transform)
}

fun <T, R> combineDataResourceFlows(
    flows: Iterable<Flow<DataResource<T>>>,
    transform: (List<T>) -> R
): Flow<DataResource<R>> = combine(flows) {
    combineDataResources(it.toList(), transform)
}

fun <T1, T2, R> combineDataResources(
    r1: DataResource<T1>,
    r2: DataResource<T2>,
    transform: (T1, T2) -> R
): DataResource<R> {
    val results = listOf(r1, r2)

    return when {
        results.anyLoading() -> DataResource.Loading
        results.anyError() -> DataResource.Error(results.getFirstError().error)
        else -> {
            r1 as DataResource.Data
            r2 as DataResource.Data

            DataResource.Data(transform(r1.data, r2.data))
        }
    }
}

fun <T1, T2, T3, R> combineDataResources(
    r1: DataResource<T1>,
    r2: DataResource<T2>,
    r3: DataResource<T3>,
    transform: (T1, T2, T3) -> R
): DataResource<R> {
    val results = listOf(r1, r2, r3)

    return when {
        results.anyLoading() -> DataResource.Loading
        results.anyError() -> DataResource.Error(results.getFirstError().error)
        else -> {
            r1 as DataResource.Data
            r2 as DataResource.Data
            r3 as DataResource.Data

            DataResource.Data(transform(r1.data, r2.data, r3.data))
        }
    }
}

fun <T1, T2, T3, T4, R> combineDataResources(
    r1: DataResource<T1>,
    r2: DataResource<T2>,
    r3: DataResource<T3>,
    r4: DataResource<T4>,
    transform: (T1, T2, T3, T4) -> R
): DataResource<R> {
    val results = listOf(r1, r2, r3, r4)

    return when {
        results.anyLoading() -> DataResource.Loading
        results.anyError() -> DataResource.Error(results.getFirstError().error)
        else -> {
            r1 as DataResource.Data
            r2 as DataResource.Data
            r3 as DataResource.Data
            r4 as DataResource.Data

            DataResource.Data(transform(r1.data, r2.data, r3.data, r4.data))
        }
    }
}

fun <T1, T2, T3, T4, T5, R> combineDataResources(
    r1: DataResource<T1>,
    r2: DataResource<T2>,
    r3: DataResource<T3>,
    r4: DataResource<T4>,
    r5: DataResource<T5>,
    transform: (T1, T2, T3, T4, T5) -> R
): DataResource<R> {
    val results = listOf(r1, r2, r3, r4, r5)

    return when {
        results.anyLoading() -> DataResource.Loading
        results.anyError() -> DataResource.Error(results.getFirstError().error)
        else -> {
            r1 as DataResource.Data
            r2 as DataResource.Data
            r3 as DataResource.Data
            r4 as DataResource.Data
            r5 as DataResource.Data

            DataResource.Data(transform(r1.data, r2.data, r3.data, r4.data, r5.data))
        }
    }
}

fun <T1, T2, T3, T4, T5, T6, R> combineDataResources(
    r1: DataResource<T1>,
    r2: DataResource<T2>,
    r3: DataResource<T3>,
    r4: DataResource<T4>,
    r5: DataResource<T5>,
    r6: DataResource<T6>,
    transform: (T1, T2, T3, T4, T5, T6) -> R
): DataResource<R> {
    val results = listOf(r1, r2, r3, r4, r5, r6)

    return when {
        results.anyLoading() -> DataResource.Loading
        results.anyError() -> DataResource.Error(results.getFirstError().error)
        else -> {
            r1 as DataResource.Data
            r2 as DataResource.Data
            r3 as DataResource.Data
            r4 as DataResource.Data
            r5 as DataResource.Data
            r6 as DataResource.Data

            DataResource.Data(transform(r1.data, r2.data, r3.data, r4.data, r5.data, r6.data))
        }
    }
}

fun <T1, T2, T3, T4, T5, T6, T7, R> combineDataResources(
    r1: DataResource<T1>,
    r2: DataResource<T2>,
    r3: DataResource<T3>,
    r4: DataResource<T4>,
    r5: DataResource<T5>,
    r6: DataResource<T6>,
    r7: DataResource<T7>,
    transform: (T1, T2, T3, T4, T5, T6, T7) -> R
): DataResource<R> {
    val results = listOf(r1, r2, r3, r4, r5, r6, r7)

    return when {
        results.anyLoading() -> DataResource.Loading
        results.anyError() -> DataResource.Error(results.getFirstError().error)
        else -> {
            r1 as DataResource.Data
            r2 as DataResource.Data
            r3 as DataResource.Data
            r4 as DataResource.Data
            r5 as DataResource.Data
            r6 as DataResource.Data
            r7 as DataResource.Data

            DataResource.Data(transform(r1.data, r2.data, r3.data, r4.data, r5.data, r6.data, r7.data))
        }
    }
}

fun <T1, T2, T3, T4, T5, T6, T7, T8, R> combineDataResources(
    r1: DataResource<T1>,
    r2: DataResource<T2>,
    r3: DataResource<T3>,
    r4: DataResource<T4>,
    r5: DataResource<T5>,
    r6: DataResource<T6>,
    r7: DataResource<T7>,
    r8: DataResource<T8>,
    transform: (T1, T2, T3, T4, T5, T6, T7, T8) -> R
): DataResource<R> {
    val results = listOf(r1, r2, r3, r4, r5, r6, r7, r8)

    return when {
        results.anyLoading() -> DataResource.Loading
        results.anyError() -> DataResource.Error(results.getFirstError().error)
        else -> {
            r1 as DataResource.Data
            r2 as DataResource.Data
            r3 as DataResource.Data
            r4 as DataResource.Data
            r5 as DataResource.Data
            r6 as DataResource.Data
            r7 as DataResource.Data
            r8 as DataResource.Data

            DataResource.Data(transform(r1.data, r2.data, r3.data, r4.data, r5.data, r6.data, r7.data, r8.data))
        }
    }
}

fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> combineDataResources(
    r1: DataResource<T1>,
    r2: DataResource<T2>,
    r3: DataResource<T3>,
    r4: DataResource<T4>,
    r5: DataResource<T5>,
    r6: DataResource<T6>,
    r7: DataResource<T7>,
    r8: DataResource<T8>,
    r9: DataResource<T9>,
    transform: (T1, T2, T3, T4, T5, T6, T7, T8, T9) -> R
): DataResource<R> {
    val results = listOf(r1, r2, r3, r4, r5, r6, r7, r8, r9)

    return when {
        results.anyLoading() -> DataResource.Loading
        results.anyError() -> DataResource.Error(results.getFirstError().error)
        else -> {
            r1 as DataResource.Data
            r2 as DataResource.Data
            r3 as DataResource.Data
            r4 as DataResource.Data
            r5 as DataResource.Data
            r6 as DataResource.Data
            r7 as DataResource.Data
            r8 as DataResource.Data
            r9 as DataResource.Data

            DataResource.Data(
                transform(r1.data, r2.data, r3.data, r4.data, r5.data, r6.data, r7.data, r8.data, r9.data)
            )
        }
    }
}

fun <T, R> combineDataResources(
    dataResources: Iterable<DataResource<T>>,
    transform: (List<T>) -> R
): DataResource<R> {
    val results = dataResources.toList()

    return when {
        results.anyLoading() -> DataResource.Loading
        results.anyError() -> DataResource.Error(results.getFirstError().error)
        else -> {
            DataResource.Data(transform(results.map { (it as DataResource.Data).data }))
        }
    }
}

fun <T> DataResource<T>.updateDataWith(updated: DataResource<T>): DataResource<T> {
    return when (this) {
        DataResource.Loading -> updated
        is DataResource.Error -> updated
        is DataResource.Data -> if (updated is DataResource.Data) updated else this
    }
}

fun <T> DataResource<T>.dataOrElse(default: T): T {
    return when (this) {
        DataResource.Loading -> default
        is DataResource.Error -> default
        is DataResource.Data -> this.data
    }
}

fun <T> DataResource<T>.dataOrNull(): T? {
    return when (this) {
        DataResource.Loading -> null
        is DataResource.Error -> null
        is DataResource.Data -> this.data
    }
}

fun <T> DataResource<List<T>>.toImmutableList(): DataResource<ImmutableList<T>> {
    return map { it.toImmutableList() }
}

suspend fun <T> Flow<DataResource<T>>.firstOutcome(): Outcome<Exception, T> =
    mapNotNull {
        when (it) {
            DataResource.Loading -> null
            is DataResource.Data -> Outcome.Success(it.data)
            is DataResource.Error -> Outcome.Failure(it.error)
        }
    }.first()

/**
 * todo filter any loading and take first.
 */
fun <T : Any> Flow<DataResource<T>>.asSingle(
    dispatcher: CoroutineDispatcher = Schedulers.io().asCoroutineDispatcher()
): Single<T> = rxSingle(dispatcher) {
    when (val first = this@asSingle.filterNot { it is DataResource.Loading }.first()) {
        is DataResource.Data -> first.data
        is DataResource.Error -> throw first.error
        is DataResource.Loading -> throw IllegalStateException("Should data or error")
    }
}

fun <T : Any> Flow<DataResource<T?>>.asMaybe(
    dispatcher: CoroutineDispatcher = Schedulers.io().asCoroutineDispatcher()
): Maybe<T> = rxMaybe(dispatcher) {
    val first = this@asMaybe.filterNot { it is DataResource.Loading }.first()
    when (first) {
        is DataResource.Data -> first.data
        is DataResource.Error -> throw first.error
        is DataResource.Loading -> throw IllegalStateException("Should data or error")
    }
}

fun <T : Any> Flow<DataResource<T>>.toObservable(): Observable<T> = filterNot { it is DataResource.Loading }
    .asObservable()
    .map { storeResponse ->
        when (storeResponse) {
            is DataResource.Data -> storeResponse.data
            is DataResource.Error -> throw storeResponse.error
            DataResource.Loading -> throw IllegalStateException()
        }
    }

fun <T, R> Flow<DataResource<T>>.mapData(mapper: suspend (T) -> R): Flow<DataResource<R>> =
    map {
        when (it) {
            is DataResource.Data -> DataResource.Data(mapper(it.data))
            is DataResource.Error -> it
            is DataResource.Loading -> it
        }
    }

fun <T, R : Exception> Flow<DataResource<T>>.mapError(mapper: (Exception) -> R): Flow<DataResource<T>> =
    map {
        when (it) {
            is DataResource.Data -> it
            is DataResource.Error -> DataResource.Error(mapper(it.error))
            is DataResource.Loading -> it
        }
    }

fun <T, R> Flow<DataResource<List<T>>>.mapListData(mapper: (T) -> R): Flow<DataResource<List<R>>> =
    map {
        when (it) {
            is DataResource.Data -> DataResource.Data(it.data.map(mapper))
            is DataResource.Error -> it
            is DataResource.Loading -> it
        }
    }

fun <T, R> Flow<DataResource<List<T>>>.mapListDataNotNull(mapper: (T) -> R?): Flow<DataResource<List<R>>> =
    map {
        when (it) {
            is DataResource.Data -> DataResource.Data(it.data.mapNotNull(mapper))
            is DataResource.Error -> it
            is DataResource.Loading -> it
        }
    }

fun <T> Flow<DataResource<Iterable<T>>>.filterListData(predicate: (T) -> Boolean): Flow<DataResource<List<T>>> =
    mapData {
        it.filter(predicate)
    }

inline fun <reified T> Flow<DataResource<Iterable<*>>>.filterDataIsInstance(): Flow<DataResource<List<T>>> =
    mapData {
        it.filterIsInstance<T>()
    }

fun <T> Flow<DataResource<T>>.getDataOrThrow(): Flow<T> =
    filterNot { it is DataResource.Loading }
        .map {
            when (it) {
                is DataResource.Data -> it.data
                is DataResource.Error ->
                    throw it.error

                is DataResource.Loading -> throw IllegalStateException()
            }
        }

fun <T> Flow<DataResource<T>>.filterNotLoading(): Flow<DataResource<T>> =
    filterNot { it is DataResource.Loading }

@OptIn(ExperimentalCoroutinesApi::class)
fun <T, R> Flow<DataResource<T>>.flatMapData(mapper: suspend (T) -> Flow<DataResource<R>>): Flow<DataResource<R>> =
    flatMapLatest {
        when (it) {
            is DataResource.Data -> mapper(it.data)
            is DataResource.Error -> flowOf(it)
            is DataResource.Loading -> flowOf(it)
        }
    }

fun <T> Flow<DataResource<T>>.printlnData(toString: T.() -> String) = map {
    (it as? DataResource.Data)?.data?.let {
        println("---------->>>>>> " + toString(it))
    }
    it
}
