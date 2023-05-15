package com.dex.data

import com.blockchain.api.dex.DexTokenResponse
import com.blockchain.api.dex.DexTokensRequest
import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.SingleAccountList
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.FreshnessStrategy.Companion.withKey
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.dataOrElse
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.DexPrefs
import com.blockchain.utils.asFlow
import com.blockchain.walletmode.WalletMode
import com.dex.data.stores.DexTokensDataStorage
import com.dex.domain.DexAccount
import com.dex.domain.DexAccountsService
import com.dex.domain.DexCurrency
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
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan

@OptIn(ExperimentalCoroutinesApi::class)
class DexAccountsRepository(
    private val coincore: Coincore,
    private val currencyPrefs: CurrencyPrefs,
    private val dexPrefs: DexPrefs,
    private val assetCatalogue: AssetCatalogue,
    private val dexTokensDataStorage: DexTokensDataStorage
) : DexAccountsService {
    override fun sourceAccounts(): Flow<List<DexAccount>> =
        dexSourceAccounts(chainId = dexPrefs.selectedChainId).catch {
            emit(emptyList())
        }

    override fun destinationAccounts(): Flow<List<DexAccount>> =
        dexDestinationAccounts(chainId = dexPrefs.selectedChainId).catch {
            emit(emptyList())
        }

    override suspend fun defSourceAccount(
        chainId: Int
    ): DexAccount? {
        return dexSourceAccounts(chainId = chainId).map { accounts ->
            accounts.maxByOrNull { it.fiatBalance }
        }.firstOrNull()
    }

    override suspend fun defDestinationAccount(
        chainId: Int,
        source: DexAccount
    ): DexAccount? {
        val persistedCurrency =
            dexPrefs.selectedDestinationCurrencyTicker.takeIf { it.isNotEmpty() && it != source.currency.networkTicker }
                ?: return null
        val currency = assetCatalogue.fromNetworkTicker(persistedCurrency) ?: return null
        return dexDestinationAccounts(chainId).firstOrNull()
            ?.firstOrNull { it.currency.networkTicker == currency.networkTicker }
    }

    override fun updatePersistedDestinationAccount(dexAccount: DexAccount) {
        dexPrefs.selectedDestinationCurrencyTicker = dexAccount.currency.networkTicker
    }

    private val freshness = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)

    private fun dexAvailableTokens(
        chainId: Int
    ): Flow<List<DexTokenResponse>> =
        dexTokensDataStorage.stream(
            freshness.withKey(
                DexTokensRequest(
                    chainId = chainId
                )
            )
        ).map {
            it.dataOrElse(emptyList())
        }

    private fun dexSourceAccounts(
        chainId: Int
    ): Flow<List<DexAccount>> {
        return dexAvailableTokens(chainId = chainId).flatMapLatest { tokens ->
            activeAccounts().map {
                it.filterKeys { account ->
                    account.currency.coinNetwork?.chainId == chainId
                }.filterValues { balance ->
                    balance.total.isPositive
                }
            }.map { balancedAccounts ->
                balancedAccounts.mapNotNull { (account, balance) ->
                    val token = tokens.firstOrNull { it.symbol == account.currency.networkTicker }
                    DexAccount(
                        account = account,
                        balance = balance.total,
                        currency = DexCurrency(
                            account.currency,
                            contractAddress = account.currency.l2identifier
                                ?: token?.address
                                ?: return@mapNotNull null,
                            chainId = chainId,
                            isVerified = token?.isVerified ?: false
                        ),
                        fiatBalance = balance.totalFiat
                    )
                }
            }
        }.onEach {
            sourceAccountsCache[chainId] = it
        }.onStart {
            sourceAccountsCache[chainId]
                .takeIf { !it.isNullOrEmpty() }
                ?.let { emit(it) }
        }
    }

    private var destinationAccountsCache: MutableMap<Int, List<DexAccount>> = mutableMapOf()
    private var sourceAccountsCache: MutableMap<Int, List<DexAccount>> = mutableMapOf()

    private val allAccounts: Flow<SingleAccountList>
        get() = coincore.allWalletsInMode(WalletMode.NON_CUSTODIAL).map { it.accounts }.asFlow()

    private fun dexDestinationAccounts(
        chainId: Int
    ): Flow<List<DexAccount>> {
        return dexAvailableTokens(chainId = chainId).flatMapLatest { tokens ->
            val active = activeAccounts().map {
                it.filterKeys { account ->
                    account.currency.coinNetwork?.chainId == chainId
                }
            }
            val all = allAccounts.map {
                it.filter { account -> (account.currency as? AssetInfo)?.coinNetwork?.chainId == chainId }
            }

            active.flatMapLatest { activeAcc ->
                all.map { allAcc ->
                    allAcc.filter {
                        it.currency.networkTicker !in activeAcc.map { acc -> acc.key.currency.networkTicker }
                    }
                }.mapNotNull { accounts ->
                    accounts.map { account ->
                        val token = tokens.firstOrNull { it.symbol == account.currency.networkTicker }
                        val accountCurrency = (account.currency as? AssetInfo) ?: return@mapNotNull null
                        DexAccount(
                            account = account,
                            balance = Money.zero(account.currency),
                            fiatBalance = Money.zero(currencyPrefs.selectedFiatCurrency),
                            currency = DexCurrency(
                                account.currency as AssetInfo,
                                contractAddress = accountCurrency.l2identifier
                                    ?: token?.address
                                    ?: return@mapNotNull null,
                                chainId = accountCurrency.coinNetwork?.chainId ?: return@mapNotNull null,
                                isVerified = token?.isVerified ?: false
                            )
                        )
                    }.plus(
                        activeAcc.map { (account, balance) ->
                            val accountCurrency = (account.currency as? AssetInfo) ?: return@mapNotNull null
                            val token = tokens.firstOrNull { it.symbol == accountCurrency.networkTicker }

                            DexAccount(
                                account = account,
                                currency = DexCurrency(
                                    account.currency,
                                    contractAddress = (account.currency as? AssetInfo)?.l2identifier
                                        ?: token?.address
                                        ?: return@mapNotNull null,
                                    chainId = accountCurrency.coinNetwork?.chainId ?: return@mapNotNull null,
                                    isVerified = token?.isVerified ?: false
                                ),
                                balance = balance.total,
                                fiatBalance = balance.totalFiat
                            )
                        }
                    )
                }
            }.onEach {
                destinationAccountsCache[chainId] = it
            }
        }.onStart {
            destinationAccountsCache[chainId]
                .takeIf { !it.isNullOrEmpty() }
                ?.let { emit(it) }
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
