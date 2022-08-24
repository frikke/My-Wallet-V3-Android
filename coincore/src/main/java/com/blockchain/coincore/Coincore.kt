package com.blockchain.coincore

import com.blockchain.coincore.impl.AllCustodialWalletsAccount
import com.blockchain.coincore.impl.AllNonCustodialWalletsAccount
import com.blockchain.coincore.impl.AllWalletsAccount
import com.blockchain.coincore.impl.CryptoInterestAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.coincore.impl.TxProcessorFactory
import com.blockchain.coincore.loader.AssetCatalogueImpl
import com.blockchain.coincore.loader.AssetLoader
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.model.FundsLocks
import com.blockchain.logging.RemoteLogger
import com.blockchain.wallet.DefaultLabels
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.rx3.asObservable
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

internal class CoincoreInitFailure(msg: String, e: Throwable) : Exception(msg, e)

@Deprecated("Use result from ExchangeManager directly")
data class ExchangePriceWithDelta(
    val price: Money,
    val delta: Double,
)

class Coincore internal constructor(
    private val assetLoader: AssetLoader,
    private val assetCatalogue: AssetCatalogueImpl,
    // TODO: Build an interface on PayloadDataManager/PayloadManager for 'global' crypto calls; second password etc?
    private val payloadManager: PayloadDataManager,
    private val txProcessorFactory: TxProcessorFactory,
    private val defaultLabels: DefaultLabels,
    private val remoteLogger: RemoteLogger,
    private val bankService: BankService,
    private val walletModeService: WalletModeService,
    private val disabledEvmAssets: List<AssetInfo>,
) {
    fun getWithdrawalLocks(localCurrency: Currency): Maybe<FundsLocks> =
        if (walletModeService.enabledWalletMode().custodialEnabled) {
            bankService.getWithdrawalLocks(localCurrency).toMaybe()
        } else Maybe.empty()

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
            AllWalletsAccount(list, defaultLabels)
        }

    fun allWalletsInMode(walletMode: WalletMode): Single<AccountGroup> =
        when (walletMode) {
            WalletMode.NON_CUSTODIAL_ONLY -> allNonCustodialWallets()
            WalletMode.CUSTODIAL_ONLY -> allCustodialWallets()
            WalletMode.UNIVERSAL -> allWallets()
        }

    fun activeWalletsInMode(walletMode: WalletMode): Single<AccountGroup> {
        val assets = activeAssets(walletMode).asObservable().firstOrError()
        return assets.flatMap {
            if (it.isEmpty()) Single.just(EmptyAccountGroup)
            else
                Single.just(it).flattenAsObservable { it }.flatMapMaybe { asset ->
                    asset.accountGroup(walletMode.defaultFilter()).map { grp -> grp.accounts }
                }.reduce { a, l -> a + l }.switchIfEmpty(Single.just(emptyList()))
                    .map {
                        when (walletMode) {
                            WalletMode.UNIVERSAL -> AllWalletsAccount(it, defaultLabels)
                            WalletMode.NON_CUSTODIAL_ONLY -> AllNonCustodialWalletsAccount(it, defaultLabels)
                            WalletMode.CUSTODIAL_ONLY -> AllCustodialWalletsAccount(it, defaultLabels)
                        }
                    }
        }
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
            AllCustodialWalletsAccount(list, defaultLabels)
        }

    private fun allNonCustodialWallets(): Single<AccountGroup> =
        walletsWithFilter(filter = AssetFilter.NonCustodial).map { list ->
            AllNonCustodialWalletsAccount(list, defaultLabels)
        }

    fun walletsWithActions(
        actions: Set<AssetAction>,
        filter: AssetFilter = walletModeService.enabledWalletMode().defaultFilter(),
        sorter: AccountsSorter = { Single.just(it) },
    ): Single<SingleAccountList> =
        walletsWithFilter(filter = filter)
            .flattenAsObservable { it }
            .flatMapMaybe { account ->
                account.stateAwareActions.flatMapMaybe { availableActions ->
                    val assetActions = availableActions.filter { it.state == ActionState.Available }.map { it.action }
                    if (assetActions.containsAll(actions)) Maybe.just(account) else Maybe.empty()
                }
            }
            .toList()
            .flatMap { list ->
                sorter(list)
            }

    fun getTransactionTargets(
        sourceAccount: CryptoAccount,
        action: AssetAction,
    ): Single<SingleAccountList> {
        val sameCurrencyTransactionTargets = get(sourceAccount.currency).transactionTargets(sourceAccount)
        return when (action) {
            AssetAction.Sell -> allWallets().map { it.accounts }.map { it.filterIsInstance<FiatAccount>() }
            AssetAction.Send -> sameCurrencyTransactionTargets
            AssetAction.InterestDeposit -> sameCurrencyTransactionTargets.map {
                it.filterIsInstance<CryptoInterestAccount>()
            }
            AssetAction.InterestWithdraw -> sameCurrencyTransactionTargets.map {
                it.filterIsInstance<CustodialTradingAccount>()
            }
            AssetAction.Swap -> allWallets().map { it.accounts }
                .map {
                    it.filterIsInstance<CryptoAccount>()
                        .filterNot { account -> account is InterestAccount || account is ExchangeAccount }
                        .filterNot { account -> account.currency == sourceAccount.currency }
                        .filter { cryptoAccount ->
                            sourceAccount.isTargetAvailableForSwap(
                                target = cryptoAccount,
                                enabledWalletMode = walletModeService.enabledWalletMode()
                            )
                        }
                }
            else -> Single.just(emptyList())
        }
    }

    /**
     * When wallet is in Universal mode, you can swap from Trading to Trading, from PK to PK and from PK to Trading
     * In any other case, swap is only allowed to same Type accounts
     */
    private fun SingleAccount.isTargetAvailableForSwap(
        target: CryptoAccount,
        enabledWalletMode: WalletMode,
    ): Boolean {
        return if (enabledWalletMode == WalletMode.UNIVERSAL) {
            if (isTrading()) target.isTrading() else true
        } else
            this.isSameType(target)
    }

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

    fun getExchangePriceWithDelta(asset: AssetInfo): Single<ExchangePriceWithDelta> {
        val cryptoAsset = this[asset] as CryptoAsset
        return cryptoAsset.exchangeRate().zipWith(
            cryptoAsset.getPricesWith24hDeltaLegacy()
        ) { currentPrice, priceDelta ->
            ExchangePriceWithDelta(currentPrice.price, priceDelta.delta24h)
        }
    }

    @Suppress("SameParameterValue")
    private fun allAccounts(includeArchived: Boolean = false): Observable<SingleAccount> =
        allWallets(includeArchived).map { it.accounts }
            .flattenAsObservable { it }

    fun isLabelUnique(label: String): Single<Boolean> =
        allAccounts(true)
            .filter { a -> a.label.compareTo(label, true) == 0 }
            .toList()
            .map { it.isEmpty() }

    fun activeAssets(walletMode: WalletMode = walletModeService.enabledWalletMode()): Flow<List<Asset>> =
        assetLoader.activeAssets(walletMode)

    fun activeWallets(walletMode: WalletMode = walletModeService.enabledWalletMode()): Single<AccountGroup> =
        activeWalletsInMode(walletMode)

    fun availableCryptoAssets(): List<AssetInfo> = assetCatalogue.supportedCryptoAssets.minus(disabledEvmAssets.toSet())
}

private fun BlockchainAccount.isSameType(other: BlockchainAccount): Boolean {
    if (this is CustodialTradingAccount && other is CustodialTradingAccount) return true
    if (this is NonCustodialAccount && other is NonCustodialAccount) return true
    if (this is InterestAccount && other is InterestAccount) return true
    return false
}
