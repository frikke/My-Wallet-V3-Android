package com.blockchain.unifiedcryptowallet.data.balances

import com.blockchain.api.selfcustody.AccountInfo
import com.blockchain.api.selfcustody.BalancesResponse
import com.blockchain.api.selfcustody.CommonResponse
import com.blockchain.api.selfcustody.PubKeyInfo
import com.blockchain.api.selfcustody.SubscriptionInfo
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.logging.RemoteLogger
import com.blockchain.outcome.getOrThrow
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.firstOutcome
import com.blockchain.store.flatMapData
import com.blockchain.store.mapData
import com.blockchain.unifiedcryptowallet.domain.balances.NetworkAccountsService
import com.blockchain.unifiedcryptowallet.domain.balances.NetworkBalance
import com.blockchain.unifiedcryptowallet.domain.balances.UnifiedBalanceNotFoundException
import com.blockchain.unifiedcryptowallet.domain.balances.UnifiedBalancesService
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWallet
import com.blockchain.unifiedcryptowallet.domain.wallet.PublicKey
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach

internal class UnifiedBalancesRepository(
    private val networkAccountsService: NetworkAccountsService,
    private val unifiedBalancesSubscribeStore: UnifiedBalancesSubscribeStore,
    private val unifiedBalancesStore: UnifiedBalancesStore,
    private val assetCatalogue: AssetCatalogue,
    private val remoteLogger: RemoteLogger,
    private val currencyPrefs: CurrencyPrefs,
) : UnifiedBalancesService {
    /**
     * Specify those to get the balance of a specific Wallet.
     */
    override fun balances(wallet: NetworkWallet?): Flow<DataResource<List<NetworkBalance>>> {
        return flow {
            val networkWallets = networkAccountsService.allNetworkWallets().filterNot { it.isImported }

            val pubKeys = networkWallets.associateWith {
                it.publicKey()
            }

            subscribe(pubKeys)

            emitAll(
                unifiedBalancesStore.stream(FreshnessStrategy.Cached(true)).mapData { response ->
                    logResponse(networkWallets, response)
                    response.balances.filter {
                        if (wallet == null) true
                        else it.currency == wallet.currency.networkTicker && it.account.index == wallet.index &&
                            it.account.name == wallet.label
                    }.mapNotNull {
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
        }.catch {
            emit(DataResource.Error(it as Exception))
        }
    }

    override fun balanceForWallet(
        wallet: NetworkWallet
    ): Flow<DataResource<NetworkBalance>> {
        return balances(wallet).flatMapData {
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
        }.onEach {
            if (it is DataResource.Error) {
                remoteLogger.logException(
                    it.error,
                    "Failed to load balance for ${wallet.currency.networkTicker} at index ${wallet.index}"
                )
            }
        }
    }

    private suspend fun subscribe(networkAccountsPubKeys: Map<NetworkWallet, List<PublicKey>>): CommonResponse {

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
                }
            )
        }
        return unifiedBalancesSubscribeStore.stream(FreshnessStrategy.Cached(false).withKey(subscriptions))
            .firstOutcome()
            .getOrThrow()
    }

    private val reports = mutableListOf<String>()
    private fun logResponse(wallets: List<NetworkWallet>, response: BalancesResponse) {
        wallets.forEach {
            val walletId = "${it.currency.networkTicker} ${it.index} ${it.label}"
            if (!reports.contains(walletId) && !response.containsWallet(it)) {
                /**
                 * We use that so we dont report duplciates
                 *
                 */
                reports.add(walletId)
                remoteLogger.logException(
                    UnifiedBalanceNotFoundException(
                        it.currency.networkTicker, it.index, it.label
                    )
                )
            }
        }
    }
}

private fun BalancesResponse.containsWallet(wallet: NetworkWallet): Boolean {
    val remoteBalances = this.balances.filter { it.balance?.amount != null }
        .map { it.account.name + it.account.index + it.currency.lowercase(Locale.ROOT) }
    val localWallet = wallet.label + wallet.index + wallet.currency.networkTicker.lowercase(Locale.ROOT)
    return remoteBalances.firstOrNull { it == localWallet } != null
}
