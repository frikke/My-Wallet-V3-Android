package piuk.blockchain.android.ui.dashboard.model

import com.blockchain.analytics.Analytics
import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.fiat.LinkedBanksFactory
import com.blockchain.core.payments.PaymentsDataManager
import com.blockchain.core.payments.model.LinkBankTransfer
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.price.HistoricalRate
import com.blockchain.logging.RemoteLogger
import com.blockchain.nabu.Feature
import com.blockchain.nabu.Tier
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.NabuUserIdentity
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.OnboardingPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.isErc20
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.kotlin.zipWith
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import piuk.blockchain.android.domain.usecases.DashboardOnboardingStep
import piuk.blockchain.android.domain.usecases.GetDashboardOnboardingStepsUseCase
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.ui.dashboard.navigation.DashboardNavigationAction
import piuk.blockchain.android.ui.settings.v2.LinkablePaymentMethods
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe
import timber.log.Timber

class DashboardGroupLoadFailure(msg: String, e: Throwable) : Exception(msg, e)
class DashboardBalanceLoadFailure(msg: String, e: Throwable) : Exception(msg, e)

class DashboardActionAdapter(
    private val coincore: Coincore,
    private val payloadManager: PayloadDataManager,
    private val exchangeRates: ExchangeRatesDataManager,
    private val currencyPrefs: CurrencyPrefs,
    private val onboardingPrefs: OnboardingPrefs,
    private val custodialWalletManager: CustodialWalletManager,
    private val paymentsDataManager: PaymentsDataManager,
    private val linkedBanksFactory: LinkedBanksFactory,
    private val simpleBuyPrefs: SimpleBuyPrefs,
    private val getDashboardOnboardingStepsUseCase: GetDashboardOnboardingStepsUseCase,
    private val userIdentity: NabuUserIdentity,
    private val analytics: Analytics,
    private val remoteLogger: RemoteLogger
) {
    fun fetchActiveAssets(model: DashboardModel): Disposable =
        coincore.fiatAssets.accountGroup()
            .map { g -> g.accounts }
            .switchIfEmpty(Maybe.just(emptyList()))
            .toSingle()
            .subscribeBy(
                onSuccess = { fiatAssets ->
                    val cryptoAssets = coincore.activeCryptoAssets().map { it.assetInfo }
                    model.process(
                        DashboardIntent.UpdateAllAssetsAndBalances(
                            cryptoAssets,
                            fiatAssets.filterIsInstance<FiatAccount>()
                        )
                    )
                },
                onError = {
                    Timber.e("Error fetching active assets - $it")
                    throw it
                }
            )

    fun fetchAvailableAssets(model: DashboardModel): Disposable =
        Single.fromCallable {
            coincore.availableCryptoAssets()
        }.subscribeBy(
            onSuccess = { assets ->
                // Load the balances for the active assets for sorting based on balance
                model.process(
                    DashboardIntent.UpdateAllAssetsAndBalances(
                        assetList = coincore.activeCryptoAssets().map { it.assetInfo },
                        fiatAssetList = emptyList()
                    )
                )
                model.process(DashboardIntent.AssetListUpdate(assets))
            },
            onError = {
                Timber.e("Error fetching available assets - $it")
                throw it
            }
        )

    fun fetchAssetPrice(model: DashboardModel, asset: AssetInfo): Disposable =
        exchangeRates.getPricesWith24hDelta(asset)
            // If prices are coming in too fast, be sure not to miss any
            .toFlowable(BackpressureStrategy.BUFFER)
            .subscribeBy(
                onNext = {
                    model.process(
                        DashboardIntent.AssetPriceUpdate(
                            asset = asset,
                            prices24HrWithDelta = it
                        )
                    )
                },
                onError = { throwable ->
                    Timber.e(throwable)
                }
            )

    // We have a problem here, in that pax init depends on ETH init
    // Ultimately, we want to init metadata straight after decrypting (or creating) the wallet
    // but we can't move that somewhere sensible yet, because 2nd password. When we remove that -
    // which is on the radar - then we can clean up the entire app init sequence.
    // But for now, we'll catch any pax init failure here, unless ETH has initialised OK. And when we
    // get a valid ETH balance, will try for a PX balance. Yeah, this is a nasty hack TODO: Fix this
    fun refreshBalances(
        model: DashboardModel,
        balanceFilter: AssetFilter,
        state: DashboardState
    ): Disposable {
        val cd = CompositeDisposable()

        state.assetMapKeys
            .filter { !it.isErc20() }
            .forEach { asset ->
                cd += refreshAssetBalance(asset, model, balanceFilter)
                    .ifEthLoadedGetErc20Balance(model, balanceFilter, cd, state)
                    .ifEthFailedThenErc20Failed(asset, model, state)
                    .subscribeBy(onError = {
                        Timber.e(it)
                    })
            }

        state.fiatAssets.fiatAccounts
            .values.forEach {
                cd += refreshFiatAssetBalance(it.account, model)
            }

        return cd
    }

    fun refreshFiatBalances(
        fiatAccounts: Map<Currency, FiatBalanceInfo>,
        model: DashboardModel
    ): Disposable {
        val disposable = CompositeDisposable()
        fiatAccounts
            .values.forEach {
                disposable += refreshFiatAssetBalance(it.account, model)
            }
        return disposable
    }

    private fun Maybe<AccountGroup>.logGroupLoadError(asset: AssetInfo, filter: AssetFilter) =
        this.doOnError { e ->
            remoteLogger.logException(
                DashboardGroupLoadFailure("Cannot load group for ${asset.displayTicker} - $filter:", e)
            )
        }

    private fun Observable<AccountBalance>.logBalanceLoadError(asset: AssetInfo, filter: AssetFilter) =
        this.doOnError { e ->
            remoteLogger.logException(
                DashboardBalanceLoadFailure("Cannot load balance for ${asset.displayTicker} - $filter:", e)
            )
        }

    private fun refreshAssetBalance(
        asset: AssetInfo,
        model: DashboardModel,
        balanceFilter: AssetFilter
    ): Single<CryptoValue> =
        coincore[asset].accountGroup(balanceFilter)
            .logGroupLoadError(asset, balanceFilter)
            .flatMapObservable { group ->
                group.balance
                    .logBalanceLoadError(asset, balanceFilter)
            }
            .doOnError { e ->
                Timber.e("Failed getting balance for ${asset.displayTicker}: $e")
                model.process(DashboardIntent.BalanceUpdateError(asset))
            }
            .doOnNext { accountBalance ->
                Timber.d("Got balance for ${asset.displayTicker}")
                model.process(DashboardIntent.BalanceUpdate(asset, accountBalance))
            }
            .retryOnError()
            .firstOrError()
            .map {
                it.total as CryptoValue
            }

    private fun <T> Observable<T>.retryOnError() =
        this.retryWhen { f ->
            f.take(RETRY_COUNT)
                .delay(RETRY_INTERVAL_MS, TimeUnit.MILLISECONDS)
        }

    private fun Single<CryptoValue>.ifEthLoadedGetErc20Balance(
        model: DashboardModel,
        balanceFilter: AssetFilter,
        disposables: CompositeDisposable,
        state: DashboardState
    ) = this.doOnSuccess { value ->
        if (value.currency == CryptoCurrency.ETHER) {
            state.erc20Assets.forEach {
                disposables += refreshAssetBalance(it, model, balanceFilter)
                    .emptySubscribe()
            }
        }
    }

    private fun Single<CryptoValue>.ifEthFailedThenErc20Failed(
        asset: AssetInfo,
        model: DashboardModel,
        state: DashboardState
    ) = this.doOnError {
        if (asset.networkTicker == CryptoCurrency.ETHER.networkTicker) {
            state.erc20Assets.forEach {
                model.process(DashboardIntent.BalanceUpdateError(it))
            }
        }
    }

    private fun refreshFiatAssetBalance(
        account: FiatAccount,
        model: DashboardModel
    ): Disposable =
        account.balance
            .firstOrError() // Ideally we shouldn't need this, but we need to kill existing subs on refresh first TODO
            .subscribeBy(
                onSuccess = { balances ->
                    model.process(
                        DashboardIntent.FiatBalanceUpdate(
                            balance = balances.total,
                            fiatBalance = balances.totalFiat,
                            balanceAvailable = balances.withdrawable
                        )
                    )
                },
                onError = {
                    Timber.e("Error while loading fiat balances $it")
                }
            )

    fun refreshPrices(model: DashboardModel, crypto: AssetInfo): Disposable =
        exchangeRates.getPricesWith24hDelta(crypto).firstOrError()
            .map { pricesWithDelta -> DashboardIntent.AssetPriceUpdate(crypto, pricesWithDelta) }
            .subscribeBy(
                onSuccess = { model.process(it) },
                onError = {
                    model.process(DashboardIntent.BalanceUpdateError(crypto))
                }
            )

    fun refreshPriceHistory(model: DashboardModel, asset: AssetInfo): Disposable =
        if (asset.startDate != null) {
            (coincore[asset] as CryptoAsset).lastDayTrend()
        } else {
            Single.just(FLATLINE_CHART)
        }.map { DashboardIntent.PriceHistoryUpdate(asset, it) }
            .subscribeBy(
                onSuccess = { model.process(it) },
                onError = { Timber.e(it) }
            )

    fun checkForCustodialBalance(model: DashboardModel, crypto: AssetInfo): Disposable {
        return coincore[crypto].accountGroup(AssetFilter.Custodial)
            .flatMapObservable { it.balance }
            .toFlowable(BackpressureStrategy.BUFFER)
            .subscribeBy(
                onNext = {
                    model.process(DashboardIntent.UpdateHasCustodialBalanceIntent(crypto, !it.total.isZero))
                },
                onComplete = {
                    model.process(DashboardIntent.UpdateHasCustodialBalanceIntent(crypto, false))
                },
                onError = { model.process(DashboardIntent.UpdateHasCustodialBalanceIntent(crypto, false)) }
            )
    }

    fun hasUserBackedUp(): Single<Boolean> = Single.just(payloadManager.isBackedUp)

    fun cancelSimpleBuyOrder(orderId: String): Disposable {
        return custodialWalletManager.deleteBuyOrder(orderId)
            .subscribeBy(
                onComplete = { simpleBuyPrefs.clearBuyState() },
                onError = { error ->
                    analytics.logEvent(SimpleBuyAnalytics.BANK_DETAILS_CANCEL_ERROR)
                    Timber.e(error)
                }
            )
    }

    fun canDeposit(): Single<Boolean> =
        userIdentity.getHighestApprovedKycTier()
            .flatMap {
                if (it == Tier.GOLD) {
                    paymentsDataManager.canTransactWithBankMethods(currencyPrefs.selectedFiatCurrency)
                } else Single.just(true)
            }

    fun launchBankTransferFlow(model: DashboardModel, currencyCode: String = "", action: AssetAction) =
        userIdentity.isEligibleFor(Feature.SimpleBuy)
            .zipWith(coincore.fiatAssets.accountGroup().toSingle())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { (isEligible, fiatGroup) ->
                    model.process(
                        if (isEligible) {
                            val networkTicker = if (currencyCode.isNotEmpty()) {
                                currencyCode
                            } else {
                                currencyPrefs.selectedFiatCurrency.networkTicker
                            }

                            val selectedAccount = fiatGroup.accounts.first {
                                (it as FiatAccount).currency.networkTicker == networkTicker
                            }

                            DashboardIntent.LaunchBankTransferFlow(
                                selectedAccount,
                                action,
                                false
                            )
                        } else {
                            DashboardIntent.ShowPortfolioSheet(DashboardNavigationAction.FiatFundsNoKyc)
                        }
                    )
                },
                onError = {
                    Timber.e(it)
                    model.process(DashboardIntent.ShowPortfolioSheet(DashboardNavigationAction.FiatFundsNoKyc))
                }
            )

    fun getBankDepositFlow(
        model: DashboardModel,
        targetAccount: SingleAccount,
        action: AssetAction,
        shouldLaunchBankLinkTransfer: Boolean
    ): Disposable {
        require(targetAccount is FiatAccount)
        return handleFiatDeposit(targetAccount, shouldLaunchBankLinkTransfer, model, action)
    }

    private fun handleFiatDeposit(
        targetAccount: FiatAccount,
        shouldLaunchBankLinkTransfer: Boolean,
        model: DashboardModel,
        action: AssetAction
    ) = Singles.zip(
        linkedBanksFactory.eligibleBankPaymentMethods(targetAccount.currency).map { paymentMethods ->
            // Ignore any WireTransferMethods In case BankLinkTransfer should launch
            paymentMethods.filter { it == PaymentMethodType.BANK_TRANSFER || !shouldLaunchBankLinkTransfer }
        },
        linkedBanksFactory.getNonWireTransferBanks().map {
            it.filter { bank -> bank.currency == targetAccount.currency }
        }
    ).doOnSubscribe {
        model.process(DashboardIntent.LongCallStarted)
    }.flatMap { (paymentMethods, linkedBanks) ->
        val eligibleBanks = linkedBanks.filter { paymentMethods.contains(it.type) }
        when {
            eligibleBanks.isEmpty() -> {
                handleNoLinkedBanks(
                    targetAccount,
                    action,
                    LinkablePaymentMethodsForAction.LinkablePaymentMethodsForDeposit(
                        linkablePaymentMethods = LinkablePaymentMethods(
                            targetAccount.currency,
                            paymentMethods.sortedBy { it.ordinal }
                        )
                    )
                )
            }
            eligibleBanks.size == 1 -> {
                Single.just(
                    FiatTransactionRequestResult.LaunchDepositFlow(
                        preselectedBankAccount = linkedBanks.first(),
                        action = action,
                        targetAccount = targetAccount
                    )
                )
            }
            else -> {
                Single.just(
                    FiatTransactionRequestResult.LaunchDepositFlowWithMultipleAccounts(
                        action = action,
                        targetAccount = targetAccount
                    )
                )
            }
        }
    }.doOnTerminate {
        model.process(DashboardIntent.LongCallEnded)
    }.subscribeBy(
        onSuccess = {
            handlePaymentMethodsUpdate(it, model, targetAccount, action)
        },
        onError = {
            Timber.e("Error loading bank transfer info $it")
        }
    )

    private fun handlePaymentMethodsUpdate(
        fiatTxRequestResult: FiatTransactionRequestResult?,
        model: DashboardModel,
        fiatAccount: FiatAccount,
        action: AssetAction
    ) {
        when (fiatTxRequestResult) {
            is FiatTransactionRequestResult.LaunchDepositFlowWithMultipleAccounts -> {
                model.process(
                    DashboardIntent.UpdateNavigationAction(
                        DashboardNavigationAction.TransactionFlow(
                            target = fiatAccount,
                            action = action
                        )
                    )
                )
            }
            is FiatTransactionRequestResult.LaunchDepositFlow -> {
                model.process(
                    DashboardIntent.UpdateNavigationAction(
                        DashboardNavigationAction.TransactionFlow(
                            target = fiatAccount,
                            sourceAccount = fiatTxRequestResult.preselectedBankAccount,
                            action = action
                        )
                    )
                )
            }
            is FiatTransactionRequestResult.LaunchWithdrawalFlowWithMultipleAccounts -> {
                model.process(
                    DashboardIntent.UpdateNavigationAction(
                        DashboardNavigationAction.TransactionFlow(
                            sourceAccount = fiatAccount,
                            action = action
                        )
                    )
                )
            }
            is FiatTransactionRequestResult.LaunchWithdrawalFlow -> {
                model.process(
                    DashboardIntent.UpdateNavigationAction(
                        DashboardNavigationAction.TransactionFlow(
                            sourceAccount = fiatAccount,
                            target = fiatTxRequestResult.preselectedBankAccount,
                            action = action
                        )
                    )
                )
            }
            is FiatTransactionRequestResult.LaunchBankLink -> {
                model.process(
                    DashboardIntent.LaunchBankLinkFlow(
                        fiatTxRequestResult.linkBankTransfer,
                        fiatAccount,
                        action
                    )
                )
            }
            is FiatTransactionRequestResult.NotSupportedPartner -> {
                // TODO Show an error
            }
            is FiatTransactionRequestResult.LaunchPaymentMethodChooser -> {
                model.process(
                    DashboardIntent.ShowLinkablePaymentMethodsSheet(
                        fiatAccount = fiatAccount,
                        paymentMethodsForAction = fiatTxRequestResult.paymentMethodForAction
                    )
                )
            }
            is FiatTransactionRequestResult.LaunchDepositDetailsSheet -> {
                model.process(DashboardIntent.ShowBankLinkingSheet(fiatTxRequestResult.targetAccount))
            }
        }
    }

    private fun handleNoLinkedBanks(
        targetAccount: FiatAccount,
        action: AssetAction,
        paymentMethodForAction: LinkablePaymentMethodsForAction
    ) =
        when {
            paymentMethodForAction.linkablePaymentMethods.linkMethods.containsAll(
                listOf(PaymentMethodType.BANK_TRANSFER, PaymentMethodType.BANK_ACCOUNT)
            ) -> {
                Single.just(
                    FiatTransactionRequestResult.LaunchPaymentMethodChooser(
                        paymentMethodForAction
                    )
                )
            }
            paymentMethodForAction.linkablePaymentMethods.linkMethods.contains(PaymentMethodType.BANK_TRANSFER) -> {
                linkBankTransfer(targetAccount.currency).map {
                    FiatTransactionRequestResult.LaunchBankLink(
                        linkBankTransfer = it,
                        action = action
                    ) as FiatTransactionRequestResult
                }.onErrorReturn {
                    FiatTransactionRequestResult.NotSupportedPartner
                }
            }
            paymentMethodForAction.linkablePaymentMethods.linkMethods.contains(PaymentMethodType.BANK_ACCOUNT) -> {
                Single.just(FiatTransactionRequestResult.LaunchDepositDetailsSheet(targetAccount))
            }
            else -> {
                Single.just(FiatTransactionRequestResult.NotSupportedPartner)
            }
        }

    fun linkBankTransfer(currency: FiatCurrency): Single<LinkBankTransfer> =
        paymentsDataManager.linkBank(currency)

    fun getBankWithdrawalFlow(
        model: DashboardModel,
        sourceAccount: SingleAccount,
        action: AssetAction,
        shouldLaunchBankLinkTransfer: Boolean
    ): Disposable {
        require(sourceAccount is FiatAccount)

        return Singles.zip(
            linkedBanksFactory.eligibleBankPaymentMethods(sourceAccount.currency as FiatCurrency)
                .map { paymentMethods ->
                    // Ignore any WireTransferMethods In case BankLinkTransfer should launch
                    paymentMethods.filter { it == PaymentMethodType.BANK_TRANSFER || !shouldLaunchBankLinkTransfer }
                },
            linkedBanksFactory.getAllLinkedBanks().map {
                it.filter { bank -> bank.currency == sourceAccount.currency }
            }
        ).flatMap { (paymentMethods, linkedBanks) ->
            when {
                linkedBanks.isEmpty() -> {
                    handleNoLinkedBanks(
                        sourceAccount,
                        action,
                        LinkablePaymentMethodsForAction.LinkablePaymentMethodsForWithdraw(
                            LinkablePaymentMethods(
                                sourceAccount.currency,
                                paymentMethods.sortedBy { it.ordinal }
                            )
                        )
                    )
                }
                linkedBanks.size == 1 -> {
                    Single.just(
                        FiatTransactionRequestResult.LaunchWithdrawalFlow(
                            preselectedBankAccount = linkedBanks.first(),
                            action = action,
                            sourceAccount = sourceAccount
                        )
                    )
                }
                else -> {
                    Single.just(
                        FiatTransactionRequestResult.LaunchWithdrawalFlowWithMultipleAccounts(
                            action = action,
                            sourceAccount = sourceAccount
                        )
                    )
                }
            }
        }.subscribeBy(
            onSuccess = {
                handlePaymentMethodsUpdate(it, model, sourceAccount, action)
            },
            onError = {
                // TODO Add error state to Dashboard
            }
        )
    }

    fun loadWithdrawalLocks(model: DashboardModel): Disposable =
        coincore.getWithdrawalLocks(currencyPrefs.selectedFiatCurrency).subscribeBy(
            onSuccess = {
                model.process(DashboardIntent.FundsLocksLoaded(it))
            },
            onError = {
                Timber.e(it)
            }
        )

    fun getOnboardingSteps(model: DashboardModel): Disposable =
        getDashboardOnboardingStepsUseCase(Unit).subscribeBy(
            onSuccess = { steps ->
                val onboardingState = if (steps.any { !it.isCompleted }) {
                    DashboardOnboardingState.Visible(steps)
                } else {
                    DashboardOnboardingState.Hidden
                }
                model.process(DashboardIntent.FetchOnboardingStepsSuccess(onboardingState))
                val hasBoughtCrypto = steps.find { it.step == DashboardOnboardingStep.BUY }?.isCompleted == true
                if (hasBoughtCrypto) onboardingPrefs.isLandingCtaDismissed = true
            },
            onError = {
                Timber.e(it)
            }
        )

    companion object {
        private val FLATLINE_CHART = listOf(
            HistoricalRate(rate = 1.0, timestamp = 0),
            HistoricalRate(rate = 1.0, timestamp = System.currentTimeMillis() / 1000)
        )

        private const val RETRY_INTERVAL_MS = 3000L
        private const val RETRY_COUNT = 3L
    }
}
