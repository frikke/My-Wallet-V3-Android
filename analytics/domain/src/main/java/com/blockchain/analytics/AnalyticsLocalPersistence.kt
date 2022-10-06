package com.blockchain.analytics

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface AnalyticsLocalPersistence {
    fun size(): Single<Long>
    fun save(item: NabuAnalyticsEvent): Completable
    fun getAllItems(): Single<List<NabuAnalyticsEvent>>
    fun getOldestItems(n: Int): Single<List<NabuAnalyticsEvent>>

    /**
     * Removes the eldest {@code n} elements.
     *
     */
    fun removeOldestItems(n: Int): Completable
    fun clear(): Completable
}
