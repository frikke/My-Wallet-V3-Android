package com.blockchain.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun <T, R> Flow<Iterable<T>>.mapList(transform: (T) -> R): Flow<List<R>> = map { list ->
    list.map(transform)
}

fun <T, R> Flow<Iterable<T>>.mapListNotNull(transform: (T) -> R?): Flow<List<R>> = map { list ->
    list.mapNotNull(transform)
}

fun <T> Flow<Iterable<T>>.filterList(predicate: (T) -> Boolean): Flow<List<T>> = map { list ->
    list.filter(predicate)
}

inline fun <reified R> Flow<Iterable<*>>.filterListItemIsInstance(): Flow<List<R>> = map { list ->
    list.filterIsInstance<R>()
}
