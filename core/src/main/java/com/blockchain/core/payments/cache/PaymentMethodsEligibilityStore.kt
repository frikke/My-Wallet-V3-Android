package com.blockchain.core.payments.cache

import com.blockchain.api.paymentmethods.models.PaymentMethodResponse
import com.blockchain.api.services.PaymentMethodsService
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

class PaymentMethodsEligibilityStore(
    private val paymentMethodsService: PaymentMethodsService
) : KeyedStore<
    PaymentMethodsEligibilityStore.Key,
    List<PaymentMethodResponse>
    > by PersistedJsonSqlDelightStoreBuilder().buildKeyed(
    storeId = STORE_ID,
    fetcher = Fetcher.Keyed.ofSingle(
        mapper = { key ->
            paymentMethodsService.getAvailablePaymentMethodsTypes(
                currency = key.currencyTicker,
                eligibleOnly = key.eligibleOnly
            )
        }
    ),
    keySerializer = Key.serializer(),
    dataSerializer = ListSerializer(PaymentMethodResponse.serializer()),
    // todo (othman) check with André about staleness strategy instead of short duration
    mediator = FreshnessMediator(Freshness.DURATION_1_HOUR)
) {

    @Serializable
    data class Key(
        val currencyTicker: String,
        val eligibleOnly: Boolean
    )

    companion object {
        private const val STORE_ID = "PaymentMethodsEligibilityStore"
    }
}
