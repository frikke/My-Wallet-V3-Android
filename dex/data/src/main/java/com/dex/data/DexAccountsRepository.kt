package com.dex.data

import com.blockchain.api.dex.DexTokensRequest
import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.SingleAccountList
import com.blockchain.core.chains.ethereum.EthDataManager.Companion.ETH_CHAIN_ID
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.data.RefreshStrategy
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.DexPrefs
import com.blockchain.store.getDataOrThrow
import com.blockchain.utils.asFlow
import com.blockchain.walletmode.WalletMode
import com.dex.data.stores.DexTokensDataStorage
import com.dex.domain.DexAccount
import com.dex.domain.DexAccountsService
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan

@OptIn(ExperimentalCoroutinesApi::class)
class DexAccountsRepository(
    private val coincore: Coincore,
    private val currencyPrefs: CurrencyPrefs,
    private val dexPrefs: DexPrefs,
    private val assetCatalogue: AssetCatalogue,
    private val dexTokensDataStorage: DexTokensDataStorage,
) : DexAccountsService {
    override fun sourceAccounts(): Flow<List<DexAccount>> =
        dexSourceAccounts().catch {
            emit(emptyList())
        }

    override suspend fun defSourceAccount(): DexAccount? {
        return dexSourceAccounts().map { accounts ->
            accounts.maxByOrNull { it.fiatBalance }
        }.firstOrNull()
    }

    override suspend fun defDestinationAccount(): DexAccount? {
        val persistedCurrency = dexPrefs.selectedDestinationCurrencyTicker.takeIf { it.isNotEmpty() } ?: return null
        val currency = assetCatalogue.fromNetworkTicker(persistedCurrency) ?: return null
        return destinationAccounts().firstOrNull()?.firstOrNull { it.currency.networkTicker == currency.networkTicker }
    }

    override fun updatePersistedDestinationAccount(dexAccount: DexAccount) {
        dexPrefs.selectedDestinationCurrencyTicker = dexAccount.currency.networkTicker
    }

    private val freshness = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)

    private fun dexAvailableTokens() =
        dexTokensDataStorage.stream(
            freshness.withKey(
                DexTokensRequest(
                    chainId = ETH_CHAIN_ID
                )
            )
        ).getDataOrThrow()

    private fun dexSourceAccounts(): Flow<List<DexAccount>> {
        return dexAvailableTokens().flatMapLatest { tokens ->
            activeAccounts().map {
                it.filterKeys { account ->
                    account.currency.coinNetwork?.chainId == ETH_CHAIN_ID
                }.filterValues { balance ->
                    balance.total.isPositive
                }
            }.map { balancedAccounts ->
                balancedAccounts.mapNotNull { (account, balance) ->
                    DexAccount(
                        account = account,
                        balance = balance.total,
                        currency = account.currency,
                        fiatBalance = balance.totalFiat,
                        contractAddress = account.currency.l2identifier
                            ?: tokens.firstOrNull { it.symbol == account.currency.networkTicker }?.address
                            ?: return@mapNotNull null,
                        chainId = ETH_CHAIN_ID
                    )
                }
            }
        }
    }

    private val allAccounts: Flow<SingleAccountList>
        get() = coincore.allWalletsInMode(WalletMode.NON_CUSTODIAL).map { it.accounts }.asFlow()

    override fun destinationAccounts(): Flow<List<DexAccount>> =
        dexAvailableTokens().flatMapLatest { tokens ->
            val active = activeAccounts().map {
                it.filterKeys { account ->
                    account.currency.networkTicker in tokens.map { token -> token.symbol }
                }
            }

            val all = allAccounts.map {
                it.filter { account ->
                    account.currency.networkTicker in tokens.map { token -> token.symbol }
                }
            }
            active.flatMapLatest { activeAcc ->
                all.map { allAcc ->
                    allAcc.filter {
                        it.currency.networkTicker !in activeAcc.map { acc -> acc.key.currency.networkTicker }
                    }
                }.map {
                    it.map { account ->
                        DexAccount(
                            account = account,
                            currency = account.currency as AssetInfo,
                            balance = Money.zero(account.currency),
                            contractAddress = tokens.first { token ->
                                token.symbol == account.currency.networkTicker
                            }.address,
                            chainId = tokens.first { token ->
                                token.symbol == account.currency.networkTicker
                            }.chainId,
                            fiatBalance = Money.zero(currencyPrefs.selectedFiatCurrency),
                        )
                    }.plus(
                        activeAcc.map { (account, balance) ->
                            DexAccount(
                                account = account,
                                currency = account.currency,
                                balance = balance.total,
                                fiatBalance = balance.totalFiat,
                                contractAddress = tokens.first { token ->
                                    token.symbol == account.currency.networkTicker
                                }.address,
                                chainId = tokens.first { token ->
                                    token.symbol == account.currency.networkTicker
                                }.chainId,
                            )
                        }
                    )
                }
            }
        }

    private fun activeAccounts() =
        coincore.activeWalletsInMode(
            walletMode = WalletMode.NON_CUSTODIAL,
            freshnessStrategy = freshness
        ).map {
            it.accounts
        }.map {
            it.filterIsInstance<CryptoAccount>()
        }.distinctUntilChanged { old, new ->
            old.map { it.currency.networkTicker }.toSet() == new.map { it.currency.networkTicker }.toSet()
        }.flatMapLatest { cryptoAccounts ->
            cryptoAccounts.map { account ->
                account.balance().map { balance -> account to balance }
            }.merge().scan(emptyMap<CryptoAccount, AccountBalance>()) { acc, (account, balance) ->
                acc
                    .filterKeys { it.currency.networkTicker != account.currency.networkTicker }
                    .plus(account to balance)
            }.filter {
                it.keys.map { acc -> acc.currency.networkTicker }.toSet() ==
                    cryptoAccounts.map { acc -> acc.currency.networkTicker }.toSet()
            }
        }
}
