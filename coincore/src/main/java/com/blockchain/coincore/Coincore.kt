package com.blockchain.coincore

import com.blockchain.coincore.fiat.FiatAsset
import com.blockchain.coincore.impl.AllCustodialWalletsAccount
import com.blockchain.coincore.impl.AllNonCustodialWalletsAccount
import com.blockchain.coincore.impl.AllWalletsAccount
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
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.model.FundsLocks
import com.blockchain.domain.wallet.CoinNetwork
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.logging.RemoteLogger
import com.blockchain.outcome.Outcome
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.store.firstOutcome
import com.blockchain.unifiedcryptowallet.domain.balances.CoinNetworksService
import com.blockchain.unifiedcryptowallet.domain.balances.NetworkAccountsService
import com.blockchain.unifiedcryptowallet.domain.wallet.NetworkWallet
import com.blockchain.utils.toFlowDataResource
import com.blockchain.wallet.DefaultLabels
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.rx3.asFlow
import kotlinx.coroutines.rx3.asObservable
import kotlinx.coroutines.rx3.await

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
    private val walletModeService: WalletModeService,
    private val ethLayerTwoFF: FeatureFlag
) {
    @Deprecated("use flow getWithdrawalLocks")
    fun getWithdrawalLocksLegacy(localCurrency: Currency): Maybe<FundsLocks> =
        walletModeService.walletModeSingle.flatMapMaybe {
            if (it.custodialEnabled) {
                bankService.getWithdrawalLocksLegacy(localCurrency).toMaybe()
            } else
                Maybe.empty()
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getWithdrawalLocks(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(forceRefresh = false),
        localCurrency: Currency
    ): Flow<DataResource<FundsLocks?>> {
        return walletModeService.walletMode.flatMapLatest { walletMode ->
            when (walletMode) {
                WalletMode.CUSTODIAL_ONLY -> bankService.getWithdrawalLocks(
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
                remoteLogger.logEvent("Coincore initialisation failed! $it")
            }

    /**
     * TODO(antonis-bc) This should get removed from here. Nothing to do with coincore
     */
    fun validateSecondPassword(secondPassword: String) =
        payloadManager.validateSecondPassword(secondPassword)

    private fun allLoadedAssets() = assetLoader.loadedAssets

    fun allWallets(includeArchived: Boolean = false): Single<AccountGroup> =
        walletsWithFilter(includeArchived, AssetFilter.All).map { list ->
            AllWalletsAccount(list, defaultLabels, currencyPrefs.selectedFiatCurrency)
        }

    fun allWalletsInMode(walletMode: WalletMode): Single<AccountGroup> =
        when (walletMode) {
            WalletMode.NON_CUSTODIAL_ONLY -> allNonCustodialWallets()
            WalletMode.CUSTODIAL_ONLY -> allCustodialWallets()
            WalletMode.UNIVERSAL -> allWallets()
        }

    fun activeWalletsInModeRx(walletMode: WalletMode): Observable<AccountGroup> {
        val activeAssets = activeAssets(walletMode).asObservable()
        return activeAssets.flatMap { assets ->
            if (assets.isEmpty()) Observable.just(allWalletsGroupForAccountsAndMode(emptyList(), walletMode))
            else
                Single.just(assets).flattenAsObservable { it }.flatMapMaybe { asset ->
                    asset.accountGroup(walletMode.defaultFilter()).map { grp -> grp.accounts }
                }.reduce { a, l -> a + l }.switchIfEmpty(Single.just(emptyList()))
                    .map { accounts ->
                        allWalletsGroupForAccountsAndMode(accounts, walletMode)
                    }.toObservable()
        }
    }

    fun activeWalletsInMode(walletMode: WalletMode): Flow<AccountGroup> {
        return activeWalletsInModeRx(walletMode).asFlow()
    }

    private fun allWalletsGroupForAccountsAndMode(accounts: SingleAccountList, walletMode: WalletMode) =
        when (walletMode) {
            WalletMode.UNIVERSAL -> AllWalletsAccount(
                accounts,
                defaultLabels,
                currencyPrefs.selectedFiatCurrency
            )
            WalletMode.NON_CUSTODIAL_ONLY -> AllNonCustodialWalletsAccount(
                accounts, defaultLabels, currencyPrefs.selectedFiatCurrency
            )
            WalletMode.CUSTODIAL_ONLY -> AllCustodialWalletsAccount(
                accounts, defaultLabels, currencyPrefs.selectedFiatCurrency
            )
        }

    private fun walletsWithFilter(includeArchived: Boolean = false, filter: AssetFilter): Single<List<SingleAccount>> =
        Maybe.concat(
            allLoadedAssets().map {
                it.accountGroup(filter).map { grp -> grp.accounts }
                    .map { list ->
                        list.filter { account ->
                            (includeArchived || account !is CryptoAccount) || !account.isArchived
                        }
                    }
            }
        ).reduce { a, l -> a + l }
            .toSingle()

    private fun allCustodialWallets(): Single<AccountGroup> =
        walletsWithFilter(filter = AssetFilter.Custodial).map { list ->
            AllCustodialWalletsAccount(list, defaultLabels, currencyPrefs.selectedFiatCurrency)
        }

    private fun allNonCustodialWallets(): Single<AccountGroup> =
        walletsWithFilter(filter = AssetFilter.NonCustodial).map { list ->
            AllNonCustodialWalletsAccount(list, defaultLabels, currencyPrefs.selectedFiatCurrency)
        }

    fun walletsWithActions(
        actions: Set<AssetAction>,
        filter: AssetFilter? = null,
        sorter: AccountsSorter = { Single.just(it) },
    ): Single<SingleAccountList> {
        val f = if (filter == null)
            walletModeService.walletModeSingle.map { it.defaultFilter() }
        else Single.just(filter)

        return f.flatMap { walletsWithFilter(filter = it) }
            .flattenAsObservable { it }
            .flatMapMaybe { account ->
                account.stateAwareActions.flatMapMaybe { availableActions ->
                    val assetActions = availableActions.filter { it.state == ActionState.Available }.map { it.action }
                    if (assetActions.containsAll(actions)) {
                        Maybe.just(account)
                    } else {
                        Maybe.empty()
                    }
                }
            }
            .toList()
            .flatMap { list ->
                sorter(list)
            }
    }

    fun getTransactionTargets(
        sourceAccount: CryptoAccount,
        action: AssetAction,
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
            AssetAction.InterestWithdraw -> sameCurrencyTransactionTargets.map {
                it.filterIsInstance<CustodialTradingAccount>()
            }
            AssetAction.Swap -> allWallets().map { it.accounts }
                .map {
                    it.filterIsInstance<CryptoAccount>()
                        .filterNot { account ->
                            account is InterestAccount ||
                                account is ExchangeAccount ||
                                account is StakingAccount
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
        assetLoader.activeAssets(WalletMode.CUSTODIAL_ONLY).asObservable().firstOrError()
            .flatMap {
                val fiats = it.filterIsInstance<FiatAsset>()
                if (fiats.isEmpty())
                    return@flatMap Single.just(emptyList())

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
        address: String,
    ): Maybe<SingleAccount> =
        filterAccountsByAddress(
            this[asset].accountGroup(AssetFilter.All),
            address
        )

    private fun filterAccountsByAddress(
        accountGroup: Maybe<AccountGroup>,
        address: String,
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
                if (it.isEmpty())
                    Maybe.empty()
                else
                    Maybe.just(it.first())
            }

    fun createTransactionProcessor(
        source: BlockchainAccount,
        target: TransactionTarget,
        action: AssetAction,
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

    fun activeAssets(walletMode: WalletMode? = null): Flow<List<Asset>> {
        return flow {
            val wMode = walletMode ?: walletModeService.walletMode.first()
            emitAll(assetLoader.activeAssets(wMode))
        }
    }

    fun activeWallets(walletMode: WalletMode? = null): Single<AccountGroup> =
        (
            walletMode?.let {
                Single.just(it)
            } ?: walletModeService.walletModeSingle
            ).flatMap {
            activeWalletsInModeRx(it).firstOrError()
        }

    fun availableCryptoAssets(): Single<List<AssetInfo>> =
        ethLayerTwoFF.enabled.flatMap { isL2Enabled ->
            if (isL2Enabled) {
                Single.just(assetCatalogue.supportedCryptoAssets)
            } else {
                assetCatalogue.otherEvmAssets().map { supportedEvmAssets ->
                    assetCatalogue.supportedCryptoAssets.minus(supportedEvmAssets.toSet())
                }
            }
        }

    fun availableCryptoAssetsFlow(): Flow<DataResource<List<AssetInfo>>> = availableCryptoAssets().toFlowDataResource()

    private fun BlockchainAccount.isSameType(other: BlockchainAccount): Boolean {
        if (this is CustodialTradingAccount && other is CustodialTradingAccount) return true
        if (this is NonCustodialAccount && other is NonCustodialAccount) return true
        if (this is InterestAccount && other is InterestAccount) return true
        return false
    }
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
                    val currency = assetCatalogue.fromNetworkTicker(it.currency) ?: return@mapNotNull null
                    coincore[currency].accountGroup(AssetFilter.NonCustodial)
                        .map { group -> group.accounts.filterIsInstance<NetworkWallet>() }.switchIfEmpty(
                            Single.just(
                                emptyList()
                            )
                        ).await()
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
