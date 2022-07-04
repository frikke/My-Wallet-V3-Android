package piuk.blockchain.android.ui.dashboard.model

import com.blockchain.analytics.Analytics
import com.blockchain.coincore.AccountBalance
import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.Asset
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAsset
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.defaultFilter
import com.blockchain.coincore.fiat.FiatAsset
import com.blockchain.coincore.fiat.LinkedBanksFactory
import com.blockchain.core.nftwaitlist.domain.NftWaitlistService
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.price.HistoricalRate
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.logging.RemoteLogger
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.Tier
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.outcome.Outcome
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.NftAnnouncementPrefs
import com.blockchain.preferences.OnboardingPrefs
import com.blockchain.preferences.ReferralPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.store.KeyedStoreRequest
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatCurrency
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx3.asObservable
import kotlinx.coroutines.rx3.rxSingle
import piuk.blockchain.android.domain.usecases.DashboardOnboardingStep
import piuk.blockchain.android.domain.usecases.GetDashboardOnboardingStepsUseCase
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.ui.dashboard.WalletModeBalanceCache
import piuk.blockchain.android.ui.dashboard.navigation.DashboardNavigationAction
import piuk.blockchain.android.ui.settings.v2.LinkablePaymentMethods
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe
import timber.log.Timber

class DashboardGroupLoadFailure(msg: String, e: Throwable) : Exception(msg, e)
class DashboardBalanceLoadFailure(msg: String, e: Throwable) : Exception(msg, e)

class DashboardActionInteractor(
    private val coincore: Coincore,
    private val payloadManager: PayloadDataManager,
    private val exchangeRates: ExchangeRatesDataManager,
    private val currencyPrefs: CurrencyPrefs,
    private val onboardingPrefs: OnboardingPrefs,
    private val custodialWalletManager: CustodialWalletManager,
    private val bankService: BankService,
    private val linkedBanksFactory: LinkedBanksFactory,
    private val simpleBuyPrefs: SimpleBuyPrefs,
    private val getDashboardOnboardingStepsUseCase: GetDashboardOnboardingStepsUseCase,
    private val nftWaitlistService: NftWaitlistService,
    private val nftAnnouncementPrefs: NftAnnouncementPrefs,
    private val userIdentity: UserIdentity,
    private val walletModeBalanceCache: WalletModeBalanceCache,
    private val walletModeService: WalletModeService,
    private val analytics: Analytics,
    private val remoteLogger: RemoteLogger,
    private val referralPrefs: ReferralPrefs,
) {

    private val defFilter: AssetFilter
        get() = walletModeService.enabledWalletMode().defaultFilter()

    fun fetchActiveAssets(model: DashboardModel): Disposable =
        walletModeService.walletMode.map {
            coincore.activeAssets(it)
        }.asObservable(Dispatchers.IO)
            .subscribeBy(
                onNext = { activeAssets ->
                    model.process(
                        DashboardIntent.UpdateActiveAssets(
                            activeAssets
                        )
                    )
                },
                onError = {
                    Timber.e("Error fetching active assets - $it")
                    throw it
                }
            )

    fun fetchAccounts(assets: List<Asset>, model: DashboardModel): Disposable {
        val fiatAccounts = assets.filterIsInstance<FiatAsset>().firstOrNull()?.accountGroup(defFilter) ?: Maybe.empty()

        return fiatAccounts.map { g -> g.accounts }
            .switchIfEmpty(Single.just(emptyList())).subscribeBy(
                onSuccess = { accounts ->
                    model.process(
                        DashboardIntent.UpdateAllAssetsAndBalances(
                            assetList = assets.filterIsInstance<CryptoAsset>().map { crytpoAsset ->
                                when (walletModeService.enabledWalletMode()) {
                                    WalletMode.UNIVERSAL,
                                    WalletMode.CUSTODIAL_ONLY -> BrokerageAsset(crytpoAsset.assetInfo)
                                    WalletMode.NON_CUSTODIAL_ONLY -> DefiAsset(crytpoAsset.assetInfo)
                                }
                            },
                            fiatAssetList = accounts.map { it as FiatAccount }
                        )
                    )
                }, onError = {
                Timber.e("Error fetching fiat accounts - $it")
                throw it
            }
            )
    }

    fun fetchAssetPrice(model: DashboardModel, asset: AssetInfo): Disposable =
        exchangeRates.getPricesWith24hDelta(asset)
            // If prices are coming in too fast, be sure not to miss any
            .subscribeBy(
                onNext = {
                    model.process(
                        DashboardIntent.AssetPriceWithDeltaUpdate(
                            asset = asset,
                            prices24HrWithDelta = it,
                            shouldFetchDayHistoricalPrices = false
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
        state: DashboardState,
    ): Disposable {
        val cd = CompositeDisposable()

        loadBalances(state.activeAssets.keys, model, cd)

        state.fiatAssets.fiatAccounts
            .values.forEach {
                cd += refreshFiatAssetBalance(it.account, model)
            }

        cd += warmWalletModeBalanceCache(cd)

        return cd
    }

    private fun warmWalletModeBalanceCache(cd: CompositeDisposable): Disposable {
        cd += walletModeBalanceCache.stream(
            request = KeyedStoreRequest.Cached(
                key = WalletMode.NON_CUSTODIAL_ONLY,
                forceRefresh = true
            )
        ).asObservable().emptySubscribe()

        cd += walletModeBalanceCache.stream(
            request = KeyedStoreRequest.Cached(
                key = WalletMode.CUSTODIAL_ONLY,
                forceRefresh = true
            )
        ).asObservable().emptySubscribe()

        return cd
    }

    private fun loadBalances(
        assets: Set<AssetInfo>,
        model: DashboardModel,
        cd: CompositeDisposable,
    ) {
        assets.forEach { asset ->
            cd += refreshAssetBalance(asset, model).subscribeBy(onError = {
                Timber.e(it)
            })
        }
    }

    fun refreshFiatBalances(
        fiatAccounts: Map<Currency, FiatBalanceInfo>,
        model: DashboardModel,
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
    ): Single<CryptoValue> =
        coincore[asset].accountGroup(defFilter)
            .logGroupLoadError(asset, defFilter)
            .flatMapObservable { group ->
                group.balance
                    .logBalanceLoadError(asset, defFilter)
            }
            .doOnError { e ->
                Timber.e("Failed getting balance for ${asset.displayTicker}: $e")
                model.process(DashboardIntent.BalanceUpdateError(asset))
            }
            .doOnNext { accountBalance ->
                Timber.d("Got balance for ${asset.displayTicker}")
                model.process(DashboardIntent.BalanceUpdate(asset, accountBalance))
            }
            .firstOrError()
            .map {
                it.total as CryptoValue
            }

    private fun refreshFiatAssetBalance(
        account: FiatAccount,
        model: DashboardModel,
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

    private fun refreshPricesWith24HDelta(model: DashboardModel, crypto: AssetInfo): Disposable =
        exchangeRates.getPricesWith24hDelta(crypto).firstOrError()
            .map { pricesWithDelta ->
                DashboardIntent.AssetPriceWithDeltaUpdate(crypto, pricesWithDelta, true)
            }
            .subscribeBy(
                onSuccess = { model.process(it) },
                onError = {
                    model.process(DashboardIntent.BalanceUpdateError(crypto))
                }
            )

    fun refreshPrices(model: DashboardModel, asset: DashboardAsset): Disposable =
        when (asset) {
            is BrokerageAsset -> refreshPricesWith24HDelta(model, asset.currency)
            is DefiAsset -> refreshPrice(model, asset.currency)
        }

    private fun refreshPrice(model: DashboardModel, crypto: AssetInfo): Disposable =
        exchangeRates.exchangeRateToUserFiat(crypto).firstOrError()
            .map { price ->
                DashboardIntent.AssetPriceUpdate(crypto, price)
            }
            .subscribeBy(
                onSuccess = { model.process(it) },
                onError = {
                    model.process(DashboardIntent.BalanceUpdateError(crypto))
                }
            )

    fun refreshPriceHistory(model: DashboardModel, asset: AssetInfo): Disposable =
        if (asset.startDate != null) {
            coincore[asset].lastDayTrend()
        } else {
            Single.just(FLATLINE_CHART)
        }.map { lastDayTrend ->
            DashboardIntent.PriceHistoryUpdate(asset, lastDayTrend)
        }
            .subscribeBy(
                onSuccess = { model.process(it) },
                onError = { Timber.e(it) }
            )

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
                    bankService.canTransactWithBankMethods(currencyPrefs.selectedFiatCurrency)
                } else Single.just(true)
            }

    fun launchBankTransferFlow(model: DashboardModel, currencyCode: String = "", action: AssetAction) =
        userIdentity.isEligibleFor(
            when (action) {
                AssetAction.FiatWithdraw -> Feature.WithdrawFiat
                AssetAction.FiatDeposit -> Feature.DepositFiat
                else -> throw IllegalArgumentException("$action not supported")
            }
        ).zipWith(coincore.fiatAssets.accountGroup().toSingle())
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
        shouldLaunchBankLinkTransfer: Boolean,
    ): Disposable {
        require(targetAccount is FiatAccount)
        return handleFiatDeposit(targetAccount, shouldLaunchBankLinkTransfer, model, action)
    }

    private fun handleFiatDeposit(
        targetAccount: FiatAccount,
        shouldLaunchBankLinkTransfer: Boolean,
        model: DashboardModel,
        action: AssetAction,
    ) = Singles.zip(
        userIdentity.userAccessForFeature(Feature.DepositFiat),
        linkedBanksFactory.eligibleBankPaymentMethods(targetAccount.currency).map { paymentMethods ->
            // Ignore any WireTransferMethods In case BankLinkTransfer should launch
            paymentMethods.filter { it == PaymentMethodType.BANK_TRANSFER || !shouldLaunchBankLinkTransfer }
        },
        linkedBanksFactory.getNonWireTransferBanks().map {
            it.filter { bank -> bank.currency == targetAccount.currency }
        }
    ).doOnSubscribe {
        model.process(DashboardIntent.LongCallStarted)
    }.flatMap { (eligibility, paymentMethods, linkedBanks) ->
        val eligibleBanks = linkedBanks.filter { paymentMethods.contains(it.type) }
        when {
            eligibility is FeatureAccess.Blocked && eligibility.reason is BlockedReason.Sanctions ->
                Single.just(
                    FiatTransactionRequestResult.BlockedDueToSanctions(
                        eligibility.reason as BlockedReason.Sanctions
                    )
                )
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
        action: AssetAction,
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
            is FiatTransactionRequestResult.BlockedDueToSanctions -> {
                model.process(
                    DashboardIntent.UpdateNavigationAction(
                        DashboardNavigationAction.FiatDepositOrWithdrawalBlockedDueToSanctions(
                            fiatTxRequestResult.reason
                        )
                    )
                )
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
            null -> {
            }
        }
    }

    private fun handleNoLinkedBanks(
        targetAccount: FiatAccount,
        action: AssetAction,
        paymentMethodForAction: LinkablePaymentMethodsForAction,
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
        bankService.linkBank(currency)

    fun getBankWithdrawalFlow(
        model: DashboardModel,
        sourceAccount: SingleAccount,
        action: AssetAction,
        shouldLaunchBankLinkTransfer: Boolean,
    ): Disposable {
        require(sourceAccount is FiatAccount)

        return Singles.zip(
            userIdentity.userAccessForFeature(Feature.WithdrawFiat),
            linkedBanksFactory.eligibleBankPaymentMethods(sourceAccount.currency as FiatCurrency)
                .map { paymentMethods ->
                    // Ignore any WireTransferMethods In case BankLinkTransfer should launch
                    paymentMethods.filter { it == PaymentMethodType.BANK_TRANSFER || !shouldLaunchBankLinkTransfer }
                },
            linkedBanksFactory.getAllLinkedBanks().map {
                it.filter { bank -> bank.currency == sourceAccount.currency }
            }
        ).flatMap { (eligibility, paymentMethods, linkedBanks) ->
            when {
                eligibility is FeatureAccess.Blocked && eligibility.reason is BlockedReason.Sanctions ->
                    Single.just(
                        FiatTransactionRequestResult.BlockedDueToSanctions(
                            eligibility.reason as BlockedReason.Sanctions
                        )
                    )
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
            }, onComplete = {
            model.process(DashboardIntent.FundsLocksLoaded(null))
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

    fun joinNftWaitlist(): Disposable {
        return rxSingle { nftWaitlistService.joinWaitlist() }.subscribeBy(
            onSuccess = { result ->
                nftAnnouncementPrefs.isJoinNftWaitlistSuccessful = result is Outcome.Success
            },
            onError = {
                Timber.e(it)
            }
        )
    }

    fun checkReferralSuccess(model: DashboardModel) = Completable.fromAction {
        val title = referralPrefs.referralSuccessTitle
        val body = referralPrefs.referralSuccessBody
        if (title.isNotBlank() && body.isNotBlank()) {
            model.process(DashboardIntent.ShowReferralSuccess(Pair(title, body)))
        }
    }.subscribeBy(
        onError = {
            Timber.e(it)
        }
    )

    fun dismissReferralSuccess() = Completable.fromAction {
        referralPrefs.referralSuccessTitle = ""
        referralPrefs.referralSuccessBody = ""
    }.subscribeBy(
        onError = {
            Timber.e(it)
        }
    )

    companion object {
        private val FLATLINE_CHART = listOf(
            HistoricalRate(rate = 1.0, timestamp = 0),
            HistoricalRate(rate = 1.0, timestamp = System.currentTimeMillis() / 1000)
        )
    }
}
