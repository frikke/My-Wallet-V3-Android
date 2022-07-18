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
import com.blockchain.domain.dataremediation.DataRemediationService
import com.blockchain.domain.dataremediation.model.Questionnaire
import com.blockchain.domain.dataremediation.model.QuestionnaireContext
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.extensions.exhaustive
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
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.kotlin.zipWith
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.Optional
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx3.asCoroutineDispatcher
import kotlinx.coroutines.rx3.asObservable
import kotlinx.coroutines.rx3.rxSingle
import piuk.blockchain.android.domain.usecases.DashboardOnboardingStep
import piuk.blockchain.android.domain.usecases.GetDashboardOnboardingStepsUseCase
import piuk.blockchain.android.simplebuy.DepositMethodOptionsViewed
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.simplebuy.WithdrawMethodOptionsViewed
import piuk.blockchain.android.ui.dashboard.WalletModeBalanceCache
import piuk.blockchain.android.ui.dashboard.navigation.DashboardNavigationAction
import piuk.blockchain.android.ui.settings.v2.LinkablePaymentMethods
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe
import piuk.blockchain.androidcore.utils.extensions.rxMaybeOutcome
import piuk.blockchain.androidcore.utils.extensions.zipSingles
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
    private val dataRemediationService: DataRemediationService,
    private val walletModeBalanceCache: WalletModeBalanceCache,
    private val walletModeService: WalletModeService,
    private val analytics: Analytics,
    private val remoteLogger: RemoteLogger,
    private val referralPrefs: ReferralPrefs
) {

    private val defFilter: AssetFilter
        get() = walletModeService.enabledWalletMode().defaultFilter()

    fun fetchActiveAssets(model: DashboardModel): Disposable =
        walletModeService.walletMode.map {
            coincore.activeAssets(it)
        }.asObservable()
            .subscribeBy(
                onNext = { activeAssets ->
                    compositeDisposable.clear()
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

    fun fetchAccounts(assets: List<Asset>, model: DashboardModel) {
        val fiatAccounts = assets.filterIsInstance<FiatAsset>()

        return model.process(
            DashboardIntent.UpdateAllAssetsAndBalances(
                assetList = assets.filterIsInstance<CryptoAsset>().map { crytpoAsset ->
                    when (walletModeService.enabledWalletMode()) {
                        WalletMode.UNIVERSAL,
                        WalletMode.CUSTODIAL_ONLY -> BrokerageAsset(crytpoAsset.assetInfo)
                        WalletMode.NON_CUSTODIAL_ONLY -> DefiAsset(crytpoAsset.assetInfo)
                    }
                },
                fiatAssetList = fiatAccounts.map { it.custodialAccount }
            )
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

    private val compositeDisposable = CompositeDisposable()

    fun refreshBalances(
        model: DashboardModel,
        activeAssets: Set<AssetInfo>,
        fiatAccounts: Set<FiatBalanceInfo>
    ): Disposable {
        loadBalances(activeAssets, model).forEach {
            it.addTo(compositeDisposable)
        }
        fiatAccounts.forEach {
            compositeDisposable += refreshFiatAssetBalance(it.account, model)
        }

        warmWalletModeBalanceCache().forEach {
            it.addTo(compositeDisposable)
        }

        return compositeDisposable
    }

    private fun warmWalletModeBalanceCache(): List<Disposable> {
        return listOf(
            walletModeBalanceCache.stream(
                request = KeyedStoreRequest.Cached(
                    key = WalletMode.NON_CUSTODIAL_ONLY,
                    forceRefresh = true
                )
            ).asObservable().emptySubscribe(),

            walletModeBalanceCache.stream(
                request = KeyedStoreRequest.Cached(
                    key = WalletMode.CUSTODIAL_ONLY,
                    forceRefresh = true
                )
            ).asObservable().emptySubscribe()
        )
    }

    private fun loadBalances(
        assets: Set<AssetInfo>,
        model: DashboardModel
    ): List<Disposable> {
        return assets.takeIf { it.isNotEmpty() }?.map { asset ->
            refreshAssetBalance(asset, model).subscribeBy(onError = {
                Timber.e(it)
            })
        } ?: kotlin.run {
            model.process(DashboardIntent.NoActiveAssets)
            emptyList()
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
        }.also {
            compositeDisposable += it
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
        ).zipWith(
            coincore.fiatAssets.flatMap { fiatAssets -> fiatAssets.map { it.accountGroup().toSingle() }.zipSingles() }
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { (isEligible, fiatGroups) ->
                    model.process(
                        if (isEligible) {
                            val networkTicker = if (currencyCode.isNotEmpty()) {
                                currencyCode
                            } else {
                                currencyPrefs.selectedFiatCurrency.networkTicker
                            }

                            val selectedAccount = fiatGroups.map { it.accounts }.flatten().first {
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
        shouldSkipQuestionnaire: Boolean,
    ): Disposable {
        require(targetAccount is FiatAccount)
        return handleFiatDeposit(targetAccount, shouldLaunchBankLinkTransfer, shouldSkipQuestionnaire, model, action)
    }

    private fun handleFiatDeposit(
        targetAccount: FiatAccount,
        shouldLaunchBankLinkTransfer: Boolean,
        shouldSkipQuestionnaire: Boolean,
        model: DashboardModel,
        action: AssetAction,
    ) = Singles.zip(
        getQuestionnaireIfNeeded(shouldSkipQuestionnaire, QuestionnaireContext.FIAT_DEPOSIT),
        userIdentity.userAccessForFeature(Feature.DepositFiat),
        linkedBanksFactory.eligibleBankPaymentMethods(targetAccount.currency).map { paymentMethods ->
            // Ignore any WireTransferMethods In case BankLinkTransfer should launch
            paymentMethods.filter { it == PaymentMethodType.BANK_TRANSFER || !shouldLaunchBankLinkTransfer }
        },
        linkedBanksFactory.getNonWireTransferBanks().map {
            it.filter { bank -> bank.currency == targetAccount.currency }
        }
    ) { questionnaireOpt, eligibility, paymentMethods, linkedBanks ->
        (questionnaireOpt to eligibility) to (paymentMethods to linkedBanks)
    }.doOnSubscribe {
        model.process(DashboardIntent.LongCallStarted)
    }.flatMap { (questionnaireOptAndEligibility, paymentMethodsAndLinkedBanks) ->
        val (questionnaireOpt, eligibility) = questionnaireOptAndEligibility
        val (paymentMethods, linkedBanks) = paymentMethodsAndLinkedBanks

        val eligibleBanks = linkedBanks.filter { paymentMethods.contains(it.type) }

        analytics.logEvent(DepositMethodOptionsViewed(paymentMethods.map { it.name }))

        when {
            eligibility is FeatureAccess.Blocked && eligibility.reason is BlockedReason.Sanctions ->
                Single.just(
                    FiatTransactionRequestResult.BlockedDueToSanctions(
                        eligibility.reason as BlockedReason.Sanctions
                    )
                )
            questionnaireOpt.isPresent -> Single.just(
                FiatTransactionRequestResult.LaunchQuestionnaire(
                    questionnaire = questionnaireOpt.get(),
                    callbackIntent = DashboardIntent.LaunchBankTransferFlow(
                        targetAccount,
                        action,
                        shouldLaunchBankLinkTransfer,
                        shouldSkipQuestionnaire = true
                    )
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
        fiatTxRequestResult: FiatTransactionRequestResult,
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
            is FiatTransactionRequestResult.LaunchQuestionnaire -> {
                model.process(
                    DashboardIntent.UpdateNavigationAction(
                        DashboardNavigationAction.DepositQuestionnaire(
                            questionnaire = fiatTxRequestResult.questionnaire,
                            callbackIntent = fiatTxRequestResult.callbackIntent
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
            is FiatTransactionRequestResult.LaunchAliasWithdrawal -> {
                model.process(DashboardIntent.ShowBankLinkingWithAlias(fiatTxRequestResult.targetAccount))
            }
            null -> {
            }
        }.exhaustive
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
                userIdentity.isArgentinian().flatMap { isArgentinian ->
                    if (isArgentinian && action == AssetAction.FiatWithdraw) {
                        Single.just(FiatTransactionRequestResult.LaunchAliasWithdrawal(targetAccount))
                    } else {
                        Single.just(FiatTransactionRequestResult.LaunchDepositDetailsSheet(targetAccount))
                    }
                }
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
        shouldSkipQuestionnaire: Boolean,
    ): Disposable {
        require(sourceAccount is FiatAccount)

        return Singles.zip(
            getQuestionnaireIfNeeded(shouldSkipQuestionnaire, QuestionnaireContext.FIAT_WITHDRAW),
            userIdentity.userAccessForFeature(Feature.WithdrawFiat),
            linkedBanksFactory.eligibleBankPaymentMethods(sourceAccount.currency as FiatCurrency)
                .map { paymentMethods ->
                    // Ignore any WireTransferMethods In case BankLinkTransfer should launch
                    paymentMethods.filter { it == PaymentMethodType.BANK_TRANSFER || !shouldLaunchBankLinkTransfer }
                },
            linkedBanksFactory.getAllLinkedBanks().map {
                it.filter { bank -> bank.currency == sourceAccount.currency }
            }
        ) { questionnaireOpt, eligibility, paymentMethods, linkedBanks ->
            (questionnaireOpt to eligibility) to (paymentMethods to linkedBanks)
        }.flatMap { (questionnaireOptAndEligibility, paymentMethodsAndLinkedBanks) ->
            val (questionnaireOpt, eligibility) = questionnaireOptAndEligibility
            val (paymentMethods, linkedBanks) = paymentMethodsAndLinkedBanks

            analytics.logEvent(WithdrawMethodOptionsViewed(paymentMethods.map { it.name }))

            when {
                eligibility is FeatureAccess.Blocked && eligibility.reason is BlockedReason.Sanctions ->
                    Single.just(
                        FiatTransactionRequestResult.BlockedDueToSanctions(
                            eligibility.reason as BlockedReason.Sanctions
                        )
                    )
                questionnaireOpt.isPresent -> Single.just(
                    FiatTransactionRequestResult.LaunchQuestionnaire(
                        questionnaire = questionnaireOpt.get(),
                        callbackIntent = DashboardIntent.LaunchBankTransferFlow(
                            sourceAccount,
                            action,
                            shouldLaunchBankLinkTransfer,
                            shouldSkipQuestionnaire = true
                        )
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
        walletModeService.walletMode.asObservable().flatMapSingle { walletMode ->
            if (!walletMode.custodialEnabled) {
                Single.just(DashboardOnboardingState.Hidden)
            } else
                getDashboardOnboardingStepsUseCase(Unit).doOnSuccess { steps ->
                    val hasBoughtCrypto = steps.find { it.step == DashboardOnboardingStep.BUY }?.isCompleted == true
                    if (hasBoughtCrypto) onboardingPrefs.isLandingCtaDismissed = true
                }.map { steps ->
                    if (steps.any { !it.isCompleted }) {
                        DashboardOnboardingState.Visible(steps)
                    } else {
                        DashboardOnboardingState.Hidden
                    }
                }
        }.subscribeBy(
            onNext = { onboardingState ->
                model.process(DashboardIntent.FetchOnboardingStepsSuccess(onboardingState))
            },
            onError = {
                Timber.e(it)
            }
        )

    private fun getQuestionnaireIfNeeded(
        shouldSkipQuestionnaire: Boolean,
        questionnaireContext: QuestionnaireContext,
    ): Single<Optional<Questionnaire>> =
        if (shouldSkipQuestionnaire) {
            Single.just(Optional.empty())
        } else {
            rxMaybeOutcome(Schedulers.io().asCoroutineDispatcher()) {
                dataRemediationService.getQuestionnaire(questionnaireContext)
            }.map { Optional.of(it) }
                .defaultIfEmpty(Optional.empty())
        }

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
