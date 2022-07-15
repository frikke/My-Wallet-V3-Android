package com.blockchain.coincore

import com.blockchain.coincore.impl.AllCustodialWalletsAccount
import com.blockchain.coincore.impl.AllNonCustodialWalletsAccount
import com.blockchain.coincore.impl.AllWalletsAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.coincore.impl.TxProcessorFactory
import com.blockchain.coincore.loader.AssetCatalogueImpl
import com.blockchain.coincore.loader.AssetLoader
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.model.FundsLocks
import com.blockchain.logging.RemoteLogger
import com.blockchain.preferences.CurrencyPrefs
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
    private val fiatAsset: Asset,
    private val currencyPrefs: CurrencyPrefs,
    private val remoteLogger: RemoteLogger,
    private val bankService: BankService,
    private val walletModeService: WalletModeService,
    private val disabledEvmAssets: List<AssetInfo>,
) {
    fun getWithdrawalLocks(localCurrency: Currency): Maybe<FundsLocks> =
        if (walletModeService.enabledWalletMode().custodialEnabled) {
            bankService.getWithdrawalLocks(localCurrency).toMaybe()
        } else Maybe.empty()

    operator fun get(asset: AssetInfo): CryptoAsset =
        assetLoader[asset]

    operator fun get(assetTicker: String): CryptoAsset? =
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

    val fiatAssets: Asset
        get() = fiatAsset

    /**
     * TODO(antonis-bc) This should get removed from here. Nothing to do with coincore
     */
    fun validateSecondPassword(secondPassword: String) =
        payloadManager.validateSecondPassword(secondPassword)

    private fun allLoadedAssets() = assetLoader.loadedAssets + fiatAsset

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

    fun activeWalletsInMode(walletMode: WalletMode): Single<AccountGroup> =
        Maybe.concat(
            activeAssets(walletMode).map {
                it.accountGroup(walletMode.defaultFilter()).map { grp -> grp.accounts }
            }
        ).reduce { a, l -> a + l }
            .toSingle()
            .map {
                when (walletMode) {
                    WalletMode.UNIVERSAL -> AllWalletsAccount(it, defaultLabels)
                    WalletMode.NON_CUSTODIAL_ONLY -> AllNonCustodialWalletsAccount(it, defaultLabels)
                    WalletMode.CUSTODIAL_ONLY -> AllCustodialWalletsAccount(it, defaultLabels)
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
        val sameCurrencyTransactionTargets =
            get(sourceAccount.currency).transactionTargets(sourceAccount)

        val fiatTargets = fiatAsset.accountGroup(AssetFilter.All)
            .map {
                it.accounts
            }.defaultIfEmpty(emptyList())

        val sameCurrencyPlusFiat = sameCurrencyTransactionTargets
            .zipWith(fiatTargets) { crypto, fiat ->
                crypto + fiat
            }

        return allWallets().map { it.accounts }
            .flatMap { allWallets ->
                if (action != AssetAction.Swap) {
                    sameCurrencyPlusFiat
                } else {
                    Single.just(allWallets)
                }
            }.map {
                it.filter(getActionFilter(action, sourceAccount))
                    .filter { target -> target != sourceAccount }
            }
    }

    private fun getActionFilter(
        action: AssetAction,
        sourceAccount: CryptoAccount,
    ): (SingleAccount) -> Boolean =
        when (action) {
            AssetAction.Sell -> {
                {
                    it is FiatAccount
                }
            }
            AssetAction.Swap -> {
                {
                    it is CryptoAccount &&
                        it !is InterestAccount &&
                        it.currency != sourceAccount.currency &&
                        it.filterTargetsByWalletMode(it, walletModeService.enabledWalletMode())
                }
            }
            AssetAction.Send -> {
                {
                    it !is FiatAccount
                }
            }
            AssetAction.InterestWithdraw -> {
                {
                    it is TradingAccount && it.currency == sourceAccount.currency
                }
            }
            else -> {
                { true }
            }
        }

    /**
     * When wallet is in Universal mode, you can swap from Trading to Trading, from PK to PK and from PK to Trading
     * In any other case, swap is only allowed to same Type accounts
     */
    private fun SingleAccount.filterTargetsByWalletMode(
        sourceAccount: CryptoAccount,
        enabledWalletMode: WalletMode,
    ): Boolean {
        return if (enabledWalletMode == WalletMode.UNIVERSAL) {
            if (sourceAccount.isTrading()) this.isTrading() else true
        } else
            sourceAccount.isSameType(this)
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

    fun getExchangePriceWithDelta(asset: AssetInfo): Single<ExchangePriceWithDelta> =
        this[asset].exchangeRate().zipWith(
            this[asset].getPricesWith24hDelta()
        ) { currentPrice, priceDelta ->
            ExchangePriceWithDelta(currentPrice.price, priceDelta.delta24h)
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

    /**
     * We provide here the default value so we dont have to change all the places in the code and
     * at the same time make our code to work reactively.
     */
    fun activeCryptoAssets(walletMode: WalletMode = walletModeService.enabledWalletMode()): List<CryptoAsset> =
        activeAssets(walletMode).filterIsInstance<CryptoAsset>()

    fun activeAssets(walletMode: WalletMode = walletModeService.enabledWalletMode()): List<Asset> =
        assetLoader.activeAssets(walletMode) + fiatAssets

    fun availableCryptoAssets(): List<AssetInfo> = assetCatalogue.supportedCryptoAssets.minus(disabledEvmAssets.toSet())
}

private fun BlockchainAccount.isSameType(other: BlockchainAccount): Boolean {
    if (this is CustodialTradingAccount && other is CustodialTradingAccount) return true
    if (this is NonCustodialAccount && other is NonCustodialAccount) return true
    if (this is InterestAccount && other is InterestAccount) return true
    return false
}
