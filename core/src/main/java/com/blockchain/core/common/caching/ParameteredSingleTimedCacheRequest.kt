package com.blockchain.core.common.caching

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ParameteredSingleTimedCacheRequest<INPUT, OUTPUT>(
    private val cacheLifetimeSeconds: Long,
    private val refreshFn: (INPUT) -> Single<OUTPUT>
) {
    private val expired = hashMapOf<INPUT, AtomicBoolean>()
    private val current: HashMap<INPUT, Single<OUTPUT>> = hashMapOf()

    fun getCachedSingle(input: INPUT): Single<OUTPUT> =
        Single.defer {
            if (
                expired[input] == null || expired[input]!!.get() || current[input] == null
            ) {
                expired[input]?.set(false) ?: kotlin.run {
                    expired[input] = AtomicBoolean(false)
                }
                current[input] = refreshFn.invoke(input).cache().doOnSuccess {
                    expired[input]?.set(false)
                }.doOnError {
                    expired[input]?.set(true)
                    current.remove(input)
                }

                Single.timer(cacheLifetimeSeconds, TimeUnit.SECONDS)
                    .subscribeBy(onSuccess = {
                        expired[input]?.set(true)
                        current.remove(input)
                    })
                return@defer current[input]!!
            }
            return@defer current[input]!!
        }

    fun invalidate(input: INPUT) {
        expired[input]?.set(true)
        current.remove(input)
    }

    fun invalidateAll() {
        expired.keys.forEach { expired[it]?.set(true) }
    }
}
