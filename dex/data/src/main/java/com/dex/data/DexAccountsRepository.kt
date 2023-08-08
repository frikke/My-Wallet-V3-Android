package com.dex.data

import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.getOrNull
import com.blockchain.outcome.map
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.DexPrefs
import com.blockchain.utils.asFlow
import com.blockchain.utils.awaitOutcome
import com.blockchain.walletmode.WalletMode
import com.dex.domain.DexAccount
import com.dex.domain.DexAccountsService
import com.dex.domain.DexCurrency
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CoinNetwork
import info.blockchain.balance.Money
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan

@OptIn(ExperimentalCoroutinesApi::class)
class DexAccountsRepository(
    private val coincore: Coincore,
    private val currencyPrefs: CurrencyPrefs,
    private val dexPrefs: DexPrefs,
    private val coroutineDispatcher: CoroutineDispatcher,
    private val assetCatalogue: AssetCatalogue,
) : DexAccountsService {
    override fun sourceAccounts(chainId: Int): Flow<List<DexAccount>> =
        dexSourceAccounts(chainId = chainId).catch {
            emit(emptyList())
        }.flowOn(coroutineDispatcher)

    override fun destinationAccounts(chainId: Int): Flow<List<DexAccount>> =
        dexDestinationAccounts(chainId = chainId).catch {
            emit(emptyList())
        }.flowOn(coroutineDispatcher)

    override suspend fun nativeNetworkAccount(coinNetwork: CoinNetwork): Outcome<Exception, CryptoNonCustodialAccount> {
        val nativeCurrency = assetCatalogue.assetInfoFromNetworkTicker(coinNetwork.nativeAssetTicker)
        require(nativeCurrency != null)
        return coincore[nativeCurrency].defaultAccount(AssetFilter.NonCustodial).map { it as CryptoNonCustodialAccount }
            .awaitOutcome()
    }

    override suspend fun defSourceAccount(
        coinNetwork: CoinNetwork
    ): DexAccount {
        val chainId = coinNetwork.chainId
        require(chainId != null)
        val availableSourceAccounts = dexSourceAccounts(chainId = chainId).flowOn(coroutineDispatcher).firstOrNull()

        val nativeTokenAccount =
            availableSourceAccounts?.firstOrNull { it.currency.networkTicker == coinNetwork.nativeAssetTicker }

        val maxFundedAccount =
            availableSourceAccounts?.maxByOrNull { it.fiatBalance ?: Money.zero(currencyPrefs.selectedFiatCurrency) }

        if (maxFundedAccount != null) {
            return maxFundedAccount
        }
        if (nativeTokenAccount != null)
            return nativeTokenAccount
        val nativeCurrency =
            assetCatalogue.assetInfoFromNetworkTicker(coinNetwork.nativeAssetTicker) ?: throw IllegalStateException("")
        val nativeAccount =
            coincore[nativeCurrency].defaultAccount(
                AssetFilter.NonCustodial
            ).awaitOutcome()

        return nativeAccount.getOrNull()?.let {
            val balance = it.balance().first()
            DexAccount(
                account = it as CryptoNonCustodialAccount,
                balance = balance.total,
                currency = DexCurrency(
                    currency = nativeCurrency,
                    chainId = coinNetwork.chainId!!,
                    contractAddress = nativeCurrency.l2identifier
                ),
                fiatBalance = balance.totalFiat
            )
        } ?: throw IllegalStateException("")
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

    private fun dexSourceAccounts(
        chainId: Int
    ): Flow<List<DexAccount>> {
        return activeAccounts().map {
            it.filterKeys { account ->
                account.currency.coinNetwork?.chainId == chainId
            }.filterValues { balance ->
                balance.total.isPositive
            }
        }.map { balancedAccounts ->
            balancedAccounts.mapNotNull { (account, balance) ->
                DexAccount(
                    account = account,
                    balance = balance.total,
                    currency = DexCurrency(
                        currency = account.currency,
                        contractAddress = account.currency.l2identifier,
                        chainId = chainId,
                    ),
                    fiatBalance = balance.totalFiat
                )
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

    private val allAccounts: Flow<List<CryptoNonCustodialAccount>>
        get() = coincore.allWalletsInMode(WalletMode.NON_CUSTODIAL).map { it.accounts }
            .map { it.filterIsInstance<CryptoNonCustodialAccount>() }
            .asFlow()

    private fun dexDestinationAccounts(
        chainId: Int
    ): Flow<List<DexAccount>> {
        val active = activeAccounts().map {
            it.filterKeys { account ->
                account.currency.coinNetwork?.chainId == chainId
            }
        }

        val all = allAccounts.map {
            it.filter { account -> (account.currency as? AssetInfo)?.coinNetwork?.chainId == chainId }
        }

        return active.flatMapLatest { activeAcc ->
            all.map { allAcc ->
                allAcc.filter {
                    it.currency.networkTicker !in activeAcc.map { acc -> acc.key.currency.networkTicker }
                }
            }.map { accounts ->
                accounts.mapNotNull { account ->
                    DexAccount(
                        account = account,
                        balance = Money.zero(account.currency),
                        fiatBalance = Money.zero(currencyPrefs.selectedFiatCurrency),
                        currency = DexCurrency(
                            currency = account.currency,
                            contractAddress = account.currency.l2identifier,
                            chainId = account.currency.coinNetwork?.chainId ?: return@mapNotNull null,
                        )
                    )
                }.plus(
                    activeAcc.mapNotNull { (account, balance) ->
                        val accountCurrency = (account.currency as? AssetInfo) ?: return@mapNotNull null
                        DexAccount(
                            account = account,
                            currency = DexCurrency(
                                account.currency,
                                contractAddress = accountCurrency.l2identifier,
                                chainId = accountCurrency.coinNetwork?.chainId ?: return@mapNotNull null,
                            ),
                            balance = balance.total,
                            fiatBalance = balance.totalFiat
                        )
                    }
                )
            }
        }.onEach {
            destinationAccountsCache[chainId] = it
        }.onStart {
            destinationAccountsCache[chainId]
                .takeIf { !it.isNullOrEmpty() }?.let { emit(it) }
        }
    }

    private fun activeAccounts() =
        coincore.activeWalletsInMode(
            walletMode = WalletMode.NON_CUSTODIAL,
            freshnessStrategy = freshness
        ).flowOn(coroutineDispatcher).map {
            it.accounts
        }.map {
            it.filterIsInstance<CryptoNonCustodialAccount>()
        }.distinctUntilChanged { old, new ->
            old.map { it.currency.networkTicker }.toSet() == new.map { it.currency.networkTicker }.toSet()
        }.flatMapLatest { cryptoAccounts ->
            cryptoAccounts.map { account ->
                account.balance().map { balance -> account to balance }
            }.merge().scan(emptyMap<CryptoNonCustodialAccount, AccountBalance>()) { acc, (account, balance) ->
                acc.filterKeys { it.currency.networkTicker != account.currency.networkTicker }
                    .plus(account to balance)
            }.filter {
                it.keys.map { acc -> acc.currency.networkTicker }.toSet() ==
                    cryptoAccounts.map { acc -> acc.currency.networkTicker }.toSet()
            }
        }
}
