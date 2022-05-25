package com.blockchain.core.payments.cache

import com.blockchain.api.NabuApiException
import com.blockchain.api.paymentmethods.models.PaymentMethodResponse
import com.blockchain.api.services.PaymentMethodsService
import com.blockchain.domain.paymentmethods.model.PaymentMethodsError
import com.blockchain.nabu.Authenticator
import com.blockchain.store.Fetcher
import com.blockchain.store.KeyedStore
import com.blockchain.store.impl.Freshness
import com.blockchain.store.impl.FreshnessMediator
import com.blockchain.store_caches_persistedjsonsqldelight.PersistedJsonSqlDelightStoreBuilder
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

class PaymentMethodsEligibilityStore(
    private val paymentMethodsService: PaymentMethodsService,
    private val authenticator: Authenticator
) : KeyedStore<
    PaymentMethodsEligibilityStore.Key,
    PaymentMethodsError,
    List<PaymentMethodResponse>
    > by PersistedJsonSqlDelightStoreBuilder().buildKeyed(
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
        errorMapper = {
            val error = (it as? NabuApiException)?.getErrorDescription().takeIf { !it.isNullOrBlank() } ?: it.message
            PaymentMethodsError.RequestFailed(error)
        }
    ),
    keySerializer = Key.serializer(),
    dataSerializer = ListSerializer(PaymentMethodResponse.serializer()),
    mediator = FreshnessMediator(Freshness.ofSeconds(20L))
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
