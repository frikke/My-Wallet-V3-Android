package com.blockchain.core.payments

import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.datamanagers.NabuDataUserProvider
import com.blockchain.usecases.UseCase
import io.reactivex.rxjava3.core.Single

class GetSupportedCurrenciesUseCase(
    private val nabuDataUserProvider: NabuDataUserProvider,
    private val bindFeatureFlag: FeatureFlag,
) : UseCase<Unit, Single<SupportedCurrencies>>() {

    private val bindEnabled: Single<Boolean> by lazy {
        bindFeatureFlag.enabled.cache()
    }

    override fun execute(parameter: Unit): Single<SupportedCurrencies> =
        Single.zip(nabuDataUserProvider.getUser(), bindEnabled) { user, isBindEnabled ->
            if (isBindEnabled && user.address?.countryCode == "AR") {
                SupportedCurrencies(
                    fundsCurrencies = SUPPORTED_FUNDS_CURRENCIES_LATAM,
                    wireTransferCurrencies = SUPPORTED_WIRE_TRANSFER_CURRENCIES_LATAM
                )
            } else {
                SupportedCurrencies(
                    fundsCurrencies = SUPPORTED_FUNDS_CURRENCIES,
                    wireTransferCurrencies = SUPPORTED_WIRE_TRANSFER_CURRENCIES
                )
            }
        }

    companion object {
        private val SUPPORTED_FUNDS_CURRENCIES = listOf(
            "GBP", "EUR", "USD"
        )
        private val SUPPORTED_FUNDS_CURRENCIES_LATAM = listOf(
            "USD", "ARS"
        )
        private val SUPPORTED_WIRE_TRANSFER_CURRENCIES = listOf(
            "GBP", "EUR", "USD"
        )
        private val SUPPORTED_WIRE_TRANSFER_CURRENCIES_LATAM = listOf(
            "USD", "ARS"
        )
    }
}

data class SupportedCurrencies(
    val fundsCurrencies: List<String>,
    val wireTransferCurrencies: List<String>,
)
