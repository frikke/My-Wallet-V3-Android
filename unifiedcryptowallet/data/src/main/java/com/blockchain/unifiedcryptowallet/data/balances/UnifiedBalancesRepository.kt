package com.blockchain.unifiedcryptowallet.data.balances

import com.blockchain.api.selfcustody.AccountInfo
import com.blockchain.api.selfcustody.CommonResponse
import com.blockchain.api.selfcustody.PubKeyInfo
import com.blockchain.api.selfcustody.SubscriptionInfo
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.outcome.getOrThrow
import com.blockchain.outcome.map
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.firstOutcome
import com.blockchain.unifiedcryptowallet.domain.balances.NetworkAccountsService
import com.blockchain.unifiedcryptowallet.domain.balances.NetworkBalance
import com.blockchain.unifiedcryptowallet.domain.balances.NetworkNonCustodialAccount
import com.blockchain.unifiedcryptowallet.domain.balances.UnifiedBalanceNotFoundException
import com.blockchain.unifiedcryptowallet.domain.balances.UnifiedBalancesService
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.Currency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money

internal class UnifiedBalancesRepository(
    private val networkAccountsService: NetworkAccountsService,
    private val unifiedBalancesSubscribeStore: UnifiedBalancesSubscribeStore,
    private val unifiedBalancesStore: UnifiedBalancesStore,
    private val assetCatalogue: AssetCatalogue,
    private val currencyPrefs: CurrencyPrefs,
) : UnifiedBalancesService {

    override suspend fun balances(): List<NetworkBalance> {
        val networkWallets = networkAccountsService.allWallets()

        val pubKeys = networkWallets.associateWith {
            it.publicKey()
        }
        subscribe(pubKeys)

        return unifiedBalancesStore.stream(FreshnessStrategy.Cached(false)).firstOutcome()
            .map { response ->
                response.balances.mapNotNull {
                    val currency = assetCatalogue.fromNetworkTicker(it.currency)
                    NetworkBalance(
                        currency = currency ?: return@mapNotNull null,
                        balance = it.balance?.amount?.let { amount ->
                            Money.fromMinor(currency, amount)
                        } ?: return@mapNotNull null,
                        unconfirmedBalance = it.pending?.amount?.let { amount ->
                            Money.fromMinor(currency, amount)
                        } ?: return@mapNotNull null,
                        index = it.account.index,
                        name = it.account.name,
                        exchangeRate = ExchangeRate(
                            from = currency,
                            to = currencyPrefs.selectedFiatCurrency,
                            rate = it.price
                        )
                    )
                }
            }
            .getOrThrow()
    }

    override suspend fun balanceForAccount(
        name: String,
        index: Int,
        currency: Currency
    ): NetworkBalance {
        return balances().firstOrNull {
            it.name == name && it.index == index && it.currency.networkTicker == currency.networkTicker
        } ?: throw UnifiedBalanceNotFoundException(
            currency = currency,
            name = name,
            index = index
        )
    }

    private suspend fun subscribe(networkAccountsPubKeys: Map<NetworkNonCustodialAccount, String>): CommonResponse {

        val subscriptions = networkAccountsPubKeys.keys.map {
            check(networkAccountsPubKeys[it] != null)
            SubscriptionInfo(
                it.currency.networkTicker,
                AccountInfo(
                    index = it.index,
                    name = it.label
                ),
                pubKeys = listOf(
                    PubKeyInfo(
                        pubKey = networkAccountsPubKeys[it]!!,
                        style = it.style,
                        descriptor = it.descriptor
                    )
                )
            )
        }
        return unifiedBalancesSubscribeStore.stream(FreshnessStrategy.Cached(false).withKey(subscriptions))
            .firstOutcome()
            .getOrThrow()
    }
}
