package piuk.blockchain.androidcore.utils.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun <T, R> Flow<List<T>>.mapList(func: (T) -> R): Flow<List<R>> = map { list ->
    list.map { func(it) }
}

fun <T, R> Flow<List<T>>.mapListNotNull(func: (T) -> R?): Flow<List<R>> = map { list ->
    list.mapNotNull { func(it) }
}
