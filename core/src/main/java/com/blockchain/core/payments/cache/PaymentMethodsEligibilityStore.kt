package com.blockchain.core.payments.cache

import com.blockchain.api.NabuApiException
import com.blockchain.api.paymentmethods.models.PaymentMethodResponse
import com.blockchain.api.services.PaymentMethodsService
import com.blockchain.core.payments.model.PaymentMethodsError
import com.blockchain.nabu.Authenticator
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.JsonPersistedStoreBuilder
import com.blockchain.store_persisters_sqldelight.SqlDelightStoreIdScopedPersister
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

class PaymentMethodsEligibilityStore(
    private val paymentMethodsService: PaymentMethodsService,
    private val authenticator: Authenticator
) : KeyedStore<
    PaymentMethodsEligibilityStore.Key,
    PaymentMethodsError,
    List<PaymentMethodResponse>
    > by JsonPersistedStoreBuilder().buildKeyed(
    storeId = STORE_ID,
    fetcher = Fetcher.Keyed.ofSingle(
        mapper = { key ->
            authenticator.getAuthHeader()
                .flatMap {
                    paymentMethodsService.getAvailablePaymentMethodsTypes(
                        authorization = it,
                        currency = key.currencyTicker,
                        tier = if (key.shouldFetchSddLimits) SDD_ELIGIBLE_TIER else null,
                        eligibleOnly = key.eligibleOnly
                    )
                }
        },
        errorMapper = { PaymentMethodsError.RequestFailed((it as? NabuApiException)?.getErrorDescription()) }
    ),
    persister = SqlDelightStoreIdScopedPersister.Builder(STORE_ID).build(),
    keySerializer = Key.serializer(),
    dataSerializer = ListSerializer(PaymentMethodResponse.serializer()),
    freshness = Freshness.ofSeconds(20L)
) {

    @Serializable
    data class Key(
        val currencyTicker: String,
        val eligibleOnly: Boolean,
        val shouldFetchSddLimits: Boolean
    )

    companion object {
        private const val STORE_ID = "PaymentMethodsEligibilityStore"
        private const val SDD_ELIGIBLE_TIER = 3
    }
}
