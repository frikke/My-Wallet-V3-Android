package com.blockchain.home.data.emptystate

import com.blockchain.domain.experiments.RemoteConfigService
import com.blockchain.outcome.Outcome
import kotlinx.coroutines.rx3.await
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class EmptyStateBuyAmountsRemoteConfig(
    private val remoteConfigService: RemoteConfigService,
    private val json: Json,
) {
    suspend fun getBuyAmounts(): Outcome<Exception, List<String>> {
        return remoteConfigService.getRawJson(BUY_AMOUNTS_KEY).await().let { buyAmountsJson ->
            if (buyAmountsJson.isEmpty()) {
                return Outcome.Success(emptyList())
            } else {
                try {
                    Outcome.Success(json.decodeFromString(buyAmountsJson))
                } catch (e: SerializationException) {
                    Outcome.Failure(e)
                }
            }
        }
    }

    companion object {
        private const val BUY_AMOUNTS_KEY = "blockchain_app_configuration_superapp_dashboard_empty_buy_amounts"
    }
}
