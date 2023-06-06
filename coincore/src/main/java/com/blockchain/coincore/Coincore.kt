package com.blockchain.coincore

import com.blockchain.coincore.fiat.FiatAsset
import com.blockchain.coincore.impl.AllCustodialWalletsAccount
import com.blockchain.coincore.impl.AllNonCustodialWalletsAccount
import com.blockchain.coincore.impl.AllWalletsAccount
import com.blockchain.coincore.impl.CustodialActiveRewardsAccount
import com.blockchain.coincore.impl.CustodialInterestAccount
import com.blockchain.coincore.impl.CustodialStakingAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.coincore.impl.TxProcessorFactory
import com.blockchain.coincore.loader.AssetCatalogueImpl
import com.blockchain.coincore.loader.AssetLoader
import com.blockchain.coincore.loader.DynamicAssetsService
import com.blockchain.core.payload.PayloadDataManager
import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.firstOutcome
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.model.FundsLocks
import com.blockchain.logging.RemoteLogger
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.getOrDefault
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.unifiedcryptowallet.domain.balances.CoinNetworksService
import com.blockchain.unifiedcryptowallet.domain.balances.NetworkAccountsService
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWallet
import com.blockchain.utils.awaitOutcome
import com.blockchain.wallet.DefaultLabels
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CoinNetwork
import info.blockchain.balance.Currency
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.exceptions.CompositeException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.rx3.asFlow
import kotlinx.coroutines.rx3.asObservable

internal class CoincoreInitFailure(msg: String, e: Throwable) : Exception(msg, e)

class Coincore internal constructor(
    private val assetLoader: AssetLoader,
    private val assetCatalogue: AssetCatalogueImpl,
    // TODO: Build an interface on PayloadDataManager/PayloadManager for 'global' crypto calls; second password etc?
    private val payloadManager: PayloadDataManager,
    private val txProcessorFactory: TxProcessorFactory,
    private val currencyPrefs: CurrencyPrefs,
    private val defaultLabels: DefaultLabels,
    private val remoteLogger: RemoteLogger,
    private val bankService: BankService,
    private val walletModeService: WalletModeService
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getWithdrawalLocks(
        localCurrency: Currency,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
    ): Flow<DataResource<FundsLocks?>> {
        return walletModeService.walletMode.flatMapLatest { walletMode ->
            when (walletMode) {
                WalletMode.CUSTODIAL -> bankService.getWithdrawalLocks(
                    freshnessStrategy = freshnessStrategy,
                    localCurrency = localCurrency
                )

                else -> flowOf(DataResource.Data(null))
            }
        }
    }

    operator fun get(asset: Currency): Asset =
        assetLoader[asset]

    operator fun get(assetTicker: String): Asset? =
        assetCatalogue.assetInfoFromNetworkTicker(assetTicker)?.let {
            assetLoader[it]
        }

    fun init(): Completable =
        assetLoader.initAndPreload()
            .doOnComplete {
                remoteLogger.logEvent("Coincore initialisation complete!")
            }
            .doOnError {
                (it as? CompositeException)?.let { compositeException ->
                    compositeException.exceptions.forEach { exception ->
                        remoteLogger.logEvent("Coincore initialisation failed! $exception")
                    }
                } ?: run {
                    remoteLogger.logEvent("Coincore initialisation failed! $it")
                }
            }

    /**
     * TODO(antonis-bc) This should get removed from here. Nothing to do with coincore
     */
    fun validateSecondPassword(secondPassword: String) =
        payloadManager.validateSecondPassword(secondPassword)

    private fun allLoadedAssets() = availableCryptoAssets().map { cryptos -> cryptos.map { get(it) } }

    fun allWallets(includeArchived: Boolean = false): Single<AccountGroup> =
        walletsWithFilter(includeArchived, AssetFilter.All).map { list ->
            AllWalletsAccount(list, defaultLabels, currencyPrefs.selectedFiatCurrency)
        }

    fun allWalletsInMode(walletMode: WalletMode): Single<AccountGroup> =
        when (walletMode) {
            WalletMode.NON_CUSTODIAL -> allNonCustodialWallets()
            WalletMode.CUSTODIAL -> allCustodialWallets()
        }

    fun activeWalletsInModeRx(
        walletMode: WalletMode,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.ForceRefresh
        )
    ): Observable<AccountGroup> {
        val activeAssets = activeAssets(walletMode, freshnessStrategy).asObservable()
        return activeAssets.flatMap { assets ->
            if (assets.isEmpty()) {
                Observable.just(allWalletsGroupForAccountsAndMode(emptyList(), walletMode))
            } else
                Single.just(assets).flattenAsObservable { it }.flatMapMaybe { asset ->
                    asset.accountGroup(walletMode.defaultFilter()).map { grp -> grp.accounts }
                }.reduce { a, l -> a + l }.switchIfEmpty(Single.just(emptyList()))
                    .map { accounts ->
                        allWalletsGroupForAccountsAndMode(accounts, walletMode)
                    }.toObservable()
        }
    }

    fun activeWalletsInMode(
        walletMode: WalletMode,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.ForceRefresh
        )
    ): Flow<AccountGroup> {
        return activeWalletsInModeRx(walletMode, freshnessStrategy).asFlow()
    }

    private fun allWalletsGroupForAccountsAndMode(accounts: SingleAccountList, walletMode: WalletMode) =
        when (walletMode) {
            WalletMode.NON_CUSTODIAL -> AllNonCustodialWalletsAccount(
                accounts,
                defaultLabels,
                currencyPrefs.selectedFiatCurrency
            )

            WalletMode.CUSTODIAL -> AllCustodialWalletsAccount(
                accounts,
                defaultLabels,
                currencyPrefs.selectedFiatCurrency
            )
        }

    private fun walletsWithFilter(includeArchived: Boolean = false, filter: AssetFilter): Single<List<SingleAccount>> =
        allLoadedAssets().flatMap { assets ->
            Maybe.concat(
                assets.map {
                    it.accountGroup(filter).map { grp -> grp.accounts }
                        .map { list ->
                            list.filter { account ->
                                (includeArchived || account !is CryptoAccount) || !account.isArchived
                            }
                        }
                }
            ).reduce { a, l -> a + l }
                .toSingle()
        }

    private fun availableWalletsForAction(action: AssetAction, filter: AssetFilter): Single<List<SingleAccount>> {
        return when (action) {
            AssetAction.Swap,
            AssetAction.Sell,
            AssetAction.InterestWithdraw,
            AssetAction.ActiveRewardsWithdraw,
            AssetAction.StakingWithdraw,
            AssetAction.Send -> activeWalletsInMode().map {
                it.accounts
            }

            AssetAction.StakingDeposit,
            AssetAction.InterestDeposit,
            AssetAction.ActiveRewardsDeposit -> allActiveWallets().firstOrError().map {
                it.accounts
            }

            AssetAction.ViewActivity,
            AssetAction.ViewStatement,
            AssetAction.Buy,
            AssetAction.FiatWithdraw,
            AssetAction.Receive,
            AssetAction.FiatDeposit,
            AssetAction.Sign -> walletsWithFilter(filter = filter)
        }
    }

    private fun allCustodialWallets(): Single<AccountGroup> =
        walletsWithFilter(filter = AssetFilter.Custodial).map { list ->
            AllCustodialWalletsAccount(list, defaultLabels, currencyPrefs.selectedFiatCurrency)
        }

    private fun allNonCustodialWallets(): Single<AccountGroup> =
        walletsWithFilter(filter = AssetFilter.NonCustodial).map { list ->
            AllNonCustodialWalletsAccount(list, defaultLabels, currencyPrefs.selectedFiatCurrency)
        }

    fun walletsWithAction(
        tickers: Set<Currency> = emptySet(),
        action: AssetAction,
        filter: AssetFilter? = null,
        sorter: AccountsSorter = { Single.just(it) }
    ): Single<SingleAccountList> {
        val f = if (filter == null) {
            walletModeService.walletModeSingle.map { it.defaultFilter() }
        } else Single.just(filter)

        return f.flatMap { assetFilter ->
            availableWalletsForAction(
                filter = assetFilter,
                action = action
            ).map { accounts ->
                accounts.filter { account ->
                    account.currency.networkTicker in tickers.map { it.networkTicker } ||
                        tickers.isEmpty()
                }
            }
        }.flattenAsObservable { it }
            .flatMapSingle { account ->
                account.hasActionAvailable(action).map {
                    mapOf(account to it)
                }
            }.reduce { t1, t2 -> t1 + t2 }
            .map { map -> map.filter { it.value } }.map {
                it.keys
            }
            .flatMapSingle {
                sorter(it.toList())
            }.switchIfEmpty(Single.just(emptyList()))
    }

    fun getTransactionTargets(
        sourceAccount: CryptoAccount,
        action: AssetAction
    ): Single<SingleAccountList> {
        val sameCurrencyTransactionTargets = get(sourceAccount.currency).transactionTargets(sourceAccount)
        return when (action) {
            AssetAction.Sell -> allFiats()
            AssetAction.Send -> sameCurrencyTransactionTargets
            AssetAction.InterestDeposit -> sameCurrencyTransactionTargets.map {
                it.filterIsInstance<CustodialInterestAccount>()
            }

            AssetAction.StakingDeposit -> sameCurrencyTransactionTargets.map {
                it.filterIsInstance<CustodialStakingAccount>()
            }

            AssetAction.ActiveRewardsDeposit -> sameCurrencyTransactionTargets.map {
                it.filterIsInstance<CustodialActiveRewardsAccount>()
            }

            AssetAction.InterestWithdraw -> sameCurrencyTransactionTargets.map {
                it.filterIsInstance<CustodialTradingAccount>()
            }

            AssetAction.Swap -> allWallets().map { it.accounts }
                .map {
                    it.filterIsInstance<CryptoAccount>()
                        .filterNot { account ->
                            account is EarnRewardsAccount ||
                                account is ExchangeAccount
                        }
                        .filterNot { account -> account.currency == sourceAccount.currency }
                        .filter { cryptoAccount ->
                            sourceAccount.isTargetAvailableForSwap(
                                target = cryptoAccount
                            )
                        }
                }

            else -> Single.just(emptyList())
        }
    }

    fun allFiats(): Single<List<SingleAccount>> =
        assetLoader.activeAssets(WalletMode.CUSTODIAL).asObservable().firstOrError()
            .flatMap {
                val fiats = it.filterIsInstance<FiatAsset>()
                if (fiats.isEmpty()) {
                    return@flatMap Single.just(emptyList())
                }

                Maybe.concat(
                    it.filterIsInstance<FiatAsset>().map { asset ->
                        asset.accountGroup(AssetFilter.Custodial).map { grp -> grp.accounts }
                    }
                ).reduce { a, l -> a + l }
                    .toSingle()
            }

    /**
     * When wallet is in Universal mode, you can swap from Trading to Trading, from PK to PK and from PK to Trading
     * In any other case, swap is only allowed to same Type accounts
     */
    private fun SingleAccount.isTargetAvailableForSwap(
        target: CryptoAccount
    ): Boolean =
        if (isTrading()) target.isTrading() else true

    fun findAccountByAddress(
        asset: AssetInfo,
        address: String
    ): Maybe<SingleAccount> =
        filterAccountsByAddress(
            this[asset].accountGroup(AssetFilter.All),
            address
        )

    private fun filterAccountsByAddress(
        accountGroup: Maybe<AccountGroup>,
        address: String
    ): Maybe<SingleAccount> =
        accountGroup.map {
            it.accounts
        }.flattenAsObservable { it }
            .flatMapSingle { account ->
                account.receiveAddress
                    .map { it as CryptoAddress }
                    .onErrorReturn { NullCryptoAddress }
                    .map { cryptoAccount ->
                        when {
                            cryptoAccount.address.equals(address, true) -> account
                            account.doesAddressBelongToWallet(address) -> account
                            else -> NullCryptoAccount()
                        }
                    }
            }.filter { it !is NullCryptoAccount }
            .toList()
            .flatMapMaybe {
                if (it.isEmpty()) {
                    Maybe.empty()
                } else
                    Maybe.just(it.first())
            }

    fun createTransactionProcessor(
        source: BlockchainAccount,
        target: TransactionTarget,
        action: AssetAction
    ): Single<TransactionProcessor> =
        txProcessorFactory.createProcessor(
            source,
            target,
            action
        )

    @Suppress("SameParameterValue")
    private fun allAccounts(includeArchived: Boolean = false): Observable<SingleAccount> =
        allWallets(includeArchived).map { it.accounts }
            .flattenAsObservable { it }

    fun isLabelUnique(label: String): Single<Boolean> =
        allAccounts(true)
            .filter { a -> a.label.compareTo(label, true) == 0 }
            .toList()
            .map { it.isEmpty() }

    fun activeAssets(
        walletMode: WalletMode? = null,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
    ): Flow<List<Asset>> {
        return flow {
            val wMode = walletMode ?: walletModeService.walletMode.first()
            emitAll(assetLoader.activeAssets(wMode, freshnessStrategy))
        }
    }

    fun allActiveAssets(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
    ): Flow<List<Asset>> {
        return flow {
            emitAll(assetLoader.activeAssets(freshnessStrategy))
        }
    }

    fun allActiveWallets(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES)
        )
    ): Observable<AccountGroup> {
        val activeAssets = assetLoader.activeAssets(freshnessStrategy = freshnessStrategy).asObservable()
        return activeAssets.flatMap { assets ->
            if (assets.isEmpty()) {
                Observable.just(
                    AllWalletsAccount(
                        emptyList(),
                        defaultLabels,
                        currencyPrefs.selectedFiatCurrency
                    )
                )
            } else {
                Single.just(assets).flattenAsObservable { it }.flatMapMaybe { asset ->
                    asset.accountGroup().map { grp -> grp.accounts }
                }.reduce { a, l -> a + l }.switchIfEmpty(
                    Single.just(emptyList())
                )
                    .map { accounts ->
                        AllWalletsAccount(
                            accounts,
                            defaultLabels,
                            currencyPrefs.selectedFiatCurrency
                        )
                    }.toObservable()
            }
        }
    }

    fun activeWalletsInMode(
        walletMode: WalletMode? = null,
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(5, TimeUnit.MINUTES)
        )
    ): Single<AccountGroup> = (
        walletMode?.let {
            Single.just(it)
        } ?: walletModeService.walletModeSingle
        ).flatMap {
        activeWalletsInModeRx(it, freshnessStrategy).firstOrError()
    }

    fun availableCryptoAssets(): Single<List<AssetInfo>> =
        Single.just(assetCatalogue.supportedCryptoAssets)
}

private fun SingleAccount.hasActionAvailable(action: AssetAction): Single<Boolean> {
    return stateOfAction(action).map { it == ActionState.Available }
}

internal class NetworkAccountsRepository(
    private val coinsNetworksRepository: CoinNetworksRepository,
    private val coincore: Coincore,
    private val assetCatalogue: AssetCatalogueImpl
) : NetworkAccountsService {
    override suspend fun allNetworkWallets(): List<NetworkWallet> {
        return when (val coins = coinsNetworksRepository.allCoinNetworks().firstOutcome()) {
            is Outcome.Failure -> return emptyList()
            is Outcome.Success -> {
                coins.value.mapNotNull {
                    val currency = assetCatalogue.fromNetworkTicker(it.nativeAssetTicker) ?: return@mapNotNull null
                    coincore[currency].accountGroup(AssetFilter.NonCustodial)
                        .map { group -> group.accounts.filterIsInstance<NetworkWallet>() }.switchIfEmpty(
                            Single.just(
                                emptyList()
                            )
                        ).onErrorReturn { emptyList() }.awaitOutcome().getOrDefault(emptyList())
                }.flatten()
            }
        }
    }
}

internal class CoinNetworksRepository(private val dynamicAssetService: DynamicAssetsService) : CoinNetworksService {
    override fun allCoinNetworks(): Flow<DataResource<List<CoinNetwork>>> {
        return dynamicAssetService.allNetworks()
    }
}
