package piuk.blockchain.androidcore.utils

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.atomic.AtomicLong
import piuk.blockchain.androidcore.utils.extensions.thenSingle

class RefreshUpdater<T : Any>(
    private val fnRefresh: () -> Completable,
    private val refreshInterval: Long = THIRTY_SECONDS
) {
    private val lastRefreshTime = AtomicLong(0)
    private var single: Single<T>? = null

    @Synchronized
    fun get(
        local: () -> T,
        force: Boolean = false
    ): Single<T> {
        single?.let {
            return it.map { local() }
        }
        return if (force || (System.currentTimeMillis() - refreshInterval > lastRefreshTime.get())) {
            fnRefresh().thenSingle { Single.just(local()) }.cache().doFinally {
                single = null
            }.also {
                lastRefreshTime.set(System.currentTimeMillis())
                single = it
            }
        } else {
            Single.defer { Single.just(local()) }
        }
    }

    fun reset() {
        lastRefreshTime.set(0)
    }

    companion object {
        private const val THIRTY_SECONDS: Long = 30 * 1000
    }
}
