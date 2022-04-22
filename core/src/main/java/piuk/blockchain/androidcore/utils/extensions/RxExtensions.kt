package piuk.blockchain.androidcore.utils.extensions

import io.reactivex.rxjava3.core.Single

// converts a List<Single<Items>> -> Single<List<Items>>
fun <T> List<Single<T>>.zipSingles(): Single<List<T>> {
    if (this.isEmpty()) return Single.just(emptyList())
    return Single.zip(this) {
        @Suppress("UNCHECKED_CAST")
        return@zip (it as Array<T>).toList()
    }
}
