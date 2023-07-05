package com.blockchain.unifiedcryptowallet.data.balances

import com.blockchain.api.selfcustody.AccountInfo
import com.blockchain.api.selfcustody.CommonResponse
import com.blockchain.api.selfcustody.PubKeyInfo
import com.blockchain.api.selfcustody.SubscriptionInfo
import com.blockchain.coincore.Coincore
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.combineDataResourceFlows
import com.blockchain.data.firstOutcome
import com.blockchain.data.flatMapData
import com.blockchain.data.mapData
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.toDataResource
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.unifiedcryptowallet.domain.balances.NetworkAccountsService
import com.blockchain.unifiedcryptowallet.domain.balances.NetworkBalance
import com.blockchain.unifiedcryptowallet.domain.balances.UnifiedBalanceNotFoundException
import com.blockchain.unifiedcryptowallet.domain.balances.UnifiedBalancesService
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWallet
import com.blockchain.unifiedcryptowallet.domain.wallet.PublicKey
import com.blockchain.walletmode.WalletMode
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.Currency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal class UnifiedBalancesRepository(
    private val networkAccountsService: NetworkAccountsService,
    private val unifiedBalancesSubscribeStore: UnifiedBalancesSubscribeStore,
    private val unifiedBalancesStore: UnifiedBalancesStore,
    private val assetCatalogue: AssetCatalogue,
    private val currencyPrefs: CurrencyPrefs,
    private val coincore: Coincore
) : UnifiedBalancesService {

    override fun failedBalancesCurrencies(
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<List<Currency>>> {
        val failedNetworksFlow = unifiedBalancesStore.stream(freshnessStrategy)
            .mapData { response -> response.networksStatus.filter { it.hasFailedToLoad } }

        val activeAccountCurrenciesFlow = coincore
            .activeWalletsInMode(walletMode = WalletMode.NON_CUSTODIAL, freshnessStrategy = freshnessStrategy)
            .map { DataResource.Data(it.accounts.map { it.currency }) }

        return combineDataResourceFlows(
            failedNetworksFlow,
            activeAccountCurrenciesFlow
        ) { failedNetworks, activeAccountCurrencies ->
            val failedNetworkTickers = failedNetworks.map { it.ticker }.toSet()
            val activeAccountTickers = activeAccountCurrencies.map { it.networkTicker }.toSet()

            val activeFailedNetworks = activeAccountTickers.intersect(failedNetworkTickers)

            activeAccountCurrencies.filter { activeFailedNetworks.contains(it.networkTicker) }
        }
    }

    /**
     * Specify those to get the balance of a specific Wallet.
     */
    override fun balances(
        wallet: NetworkWallet?,
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<List<NetworkBalance>>> {
        return flow {
            val pubKeys = networkAccountsService.allNetworkWallets().filterNot { it.isImported }.associateWith {
                it.publicKey()
            }
            when (val subscribeResult = subscribe(pubKeys)) {
                is Outcome.Failure -> {
                    emit(subscribeResult.toDataResource())
                }

                is Outcome.Success -> {
                    emitAll(
                        unifiedBalancesStore.stream(freshnessStrategy)
                            .mapData { response ->
                                response.balances.filter {
                                    if (wallet == null) {
                                        true
                                    } else it.currency == wallet.currency.networkTicker &&
                                        it.account.index == wallet.index
                                }.mapNotNull {
                                    if (it.price == null) return@mapNotNull null
                                    val cc = assetCatalogue.fromNetworkTicker(it.currency)
                                    NetworkBalance(
                                        currency = cc ?: return@mapNotNull null,
                                        balance = it.balance?.amount?.let { amount ->
                                            Money.fromMinor(cc, amount)
                                        } ?: return@mapNotNull null,
                                        unconfirmedBalance = it.pending?.amount?.let { amount ->
                                            Money.fromMinor(cc, amount)
                                        } ?: return@mapNotNull null,
                                        exchangeRate = ExchangeRate(
                                            from = cc,
                                            to = currencyPrefs.selectedFiatCurrency,
                                            rate = it.price
                                        )
                                    )
                                }
                            }
                    )
                }
            }
        }.catch {
            emit(DataResource.Error(it as Exception))
        }
    }

    override fun balanceForWallet(
        wallet: NetworkWallet,
        freshnessStrategy: FreshnessStrategy
    ): Flow<DataResource<NetworkBalance>> {
        return balances(wallet, freshnessStrategy).flatMapData {
            it.firstOrNull()?.let { balance ->
                flowOf(DataResource.Data(balance))
            } ?: flowOf(
                DataResource.Error(
                    UnifiedBalanceNotFoundException(
                        currency = wallet.currency.networkTicker,
                        name = wallet.label,
                        index = wallet.index
                    )
                )
            )
        }
    }

    private suspend fun subscribe(
        networkAccountsPubKeys: Map<NetworkWallet, List<PublicKey>>
    ): Outcome<Exception, CommonResponse> {
        val subscriptions = networkAccountsPubKeys.keys.map {
            check(networkAccountsPubKeys[it] != null)
            SubscriptionInfo(
                it.currency.networkTicker,
                AccountInfo(
                    index = it.index,
                    name = it.label
                ),
                pubKeys = networkAccountsPubKeys[it]!!.map { pubKey ->
                    PubKeyInfo(
                        pubKey = pubKey.address,
                        style = pubKey.style,
                        descriptor = pubKey.descriptor
                    )
                }.sortedBy { it.pubKey }
            )
        }.sortedBy { it.currency }
        return unifiedBalancesSubscribeStore.stream(
            FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
                .withKey(subscriptions)
        ).firstOutcome()
    }
}
