package com.blockchain.core.payments

import com.blockchain.api.services.CollateralLocks
import com.blockchain.api.services.PaymentsService
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.Fetcher
import com.blockchain.store.Store
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import com.blockchain.storedatasource.FlushableDataSource
import com.blockchain.utils.awaitOutcome

class WithdrawLocksStore(
    private val paymentsService: PaymentsService,
    private val currencyPrefs: CurrencyPrefs
) : Store<CollateralLocks> by PersistedJsonSqlDelightStoreBuilder()
    .build(
        storeId = STORE_ID,
        fetcher = Fetcher.ofOutcome(
            mapper = {
                paymentsService.getWithdrawalLocks(currencyPrefs.selectedFiatCurrency.networkTicker).awaitOutcome()
            }
        ),
        dataSerializer = CollateralLocks.serializer(),
        mediator = FreshnessMediator(Freshness.DURATION_24_HOURS)
    ),
    FlushableDataSource {

    override fun invalidate() {
        markAsStale()
    }

    companion object {
        private const val STORE_ID = "WithdrawLocksStore"
    }
}
