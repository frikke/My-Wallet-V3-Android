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
import com.blockchain.core.kyc.domain.KycService
import com.blockchain.core.kyc.domain.model.KycTier
import com.blockchain.core.nftwaitlist.domain.NftWaitlistService
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.price.HistoricalRate
import com.blockchain.data.DataResource
import com.blockchain.data.KeyedFreshnessStrategy
import com.blockchain.domain.dataremediation.DataRemediationService
import com.blockchain.domain.dataremediation.model.Questionnaire
import com.blockchain.domain.dataremediation.model.QuestionnaireContext
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.referral.ReferralService
import com.blockchain.domain.referral.model.ReferralInfo
import com.blockchain.extensions.exhaustive
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.logging.RemoteLogger
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.outcome.Outcome
import com.blockchain.outcome.getOrDefault
import com.blockchain.preferences.CowboysPrefs
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.NftAnnouncementPrefs
import com.blockchain.preferences.OnboardingPrefs
import com.blockchain.preferences.ReferralPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.walletmode.WalletMode
import com.blockchain.walletmode.WalletModeService
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.Money
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
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.rx3.asCoroutineDispatcher
import kotlinx.coroutines.rx3.asObservable
import kotlinx.coroutines.rx3.rxSingle
import piuk.blockchain.android.domain.usecases.DashboardOnboardingStep
import piuk.blockchain.android.domain.usecases.GetDashboardOnboardingStepsUseCase
import piuk.blockchain.android.simplebuy.DepositMethodOptionsViewed
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.simplebuy.WithdrawMethodOptionsViewed
import piuk.blockchain.android.ui.cowboys.CowboysPromoDataProvider
import piuk.blockchain.android.ui.dashboard.WalletModeBalanceCache
import piuk.blockchain.android.ui.dashboard.navigation.DashboardNavigationAction
import piuk.blockchain.android.ui.settings.v2.LinkablePaymentMethods
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe
import piuk.blockchain.androidcore.utils.extensions.rxMaybeOutcome
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
    private val kycService: KycService,
    private val dataRemediationService: DataRemediationService,
    private val walletModeBalanceCache: WalletModeBalanceCache,
    private val walletModeService: WalletModeService,
    private val analytics: Analytics,
    private val remoteLogger: RemoteLogger,
    private val referralPrefs: ReferralPrefs,
    private val cowboysFeatureFlag: FeatureFlag,
    private val settingsDataManager: SettingsDataManager,
    private val cowboysDataProvider: CowboysPromoDataProvider,
    private val referralService: ReferralService,
    private val cowboysPrefs: CowboysPrefs
) {

    private val defFilter: AssetFilter
        get() = walletModeService.enabledWalletMode().defaultFilter()

    fun fetchActiveAssets(model: DashboardModel): Disposable =
        walletModeService.walletMode.onEach {
            model.process(DashboardIntent.ClearAnnouncement)
        }.flatMapLatest {
            coincore.activeAssets(it)
        }.distinctUntilChangedBy { c -> c.map { it.currency } }.asObservable().subscribeBy(
            onNext = { activeAssets ->
                Timber.v("Active assets ${activeAssets.map { it.currency.networkTicker }}")
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
        ).also {
            compositeDisposable.clear()
        }.also {
            compositeDisposable.add(it)
        }

    fun fetchAccounts(assets: List<Asset>, model: DashboardModel) {
        return model.process(
            DashboardIntent.UpdateAllAssetsAndBalances(
                assetList = assets.map { asset ->
                    asset.toDashboardAsset(walletModeService.enabledWalletMode())
                }
            )
        )
    }

    private fun Asset.toDashboardAsset(walletMode: WalletMode): DashboardAsset {
        return when (this) {
            is CryptoAsset -> when (walletMode) {
                WalletMode.UNIVERSAL,
                WalletMode.CUSTODIAL_ONLY -> BrokerageCryptoAsset(this.currency)
                WalletMode.NON_CUSTODIAL_ONLY -> DefiAsset(this.currency)
            }
            is FiatAsset -> when (walletMode) {
                WalletMode.UNIVERSAL,
                WalletMode.CUSTODIAL_ONLY -> BrokearageFiatAsset(
                    currency = this.currency,
                    fiatAccount = this.custodialAccount
                )
                WalletMode.NON_CUSTODIAL_ONLY -> throw IllegalStateException(
                    "fiats are not supported in Non custodial mode"
                )
            }
            else -> throw IllegalArgumentException("$this is not a know asset type for dashboard")
        }
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
        activeAssets: Set<Currency>,
    ): Disposable {
        loadBalances(activeAssets, model).forEach {
            it.addTo(compositeDisposable)
        }
        warmWalletModeBalanceCache().forEach {
            it.addTo(compositeDisposable)
        }

        return compositeDisposable
    }

    private fun warmWalletModeBalanceCache(): List<Disposable> {
        return listOf(
            walletModeBalanceCache.stream(
                request = KeyedFreshnessStrategy.Cached(
                    key = WalletMode.NON_CUSTODIAL_ONLY,
                    forceRefresh = false
                )
            ).asObservable().emptySubscribe(),

            walletModeBalanceCache.stream(
                request = KeyedFreshnessStrategy.Cached(
                    key = WalletMode.CUSTODIAL_ONLY,
                    forceRefresh = false
                )
            ).asObservable().emptySubscribe()
        )
    }

    private fun loadBalances(
        assets: Set<Currency>,
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

    private fun Maybe<AccountGroup>.logGroupLoadError(asset: Currency, filter: AssetFilter) =
        this.doOnError { e ->
            remoteLogger.logException(
                DashboardGroupLoadFailure("Cannot load group for ${asset.displayTicker} - $filter:", e)
            )
        }

    private fun Observable<AccountBalance>.logBalanceLoadError(asset: Currency, filter: AssetFilter) =
        this.doOnError { e ->
            remoteLogger.logException(
                DashboardBalanceLoadFailure("Cannot load balance for ${asset.displayTicker} - $filter:", e)
            )
        }

    private fun refreshAssetBalance(
        currency: Currency,
        model: DashboardModel,
    ): Single<Money> =
        coincore[currency].accountGroup(defFilter)
            .logGroupLoadError(currency, defFilter)
            .flatMapObservable { group ->
                group.balance
                    .logBalanceLoadError(currency, defFilter)
            }
            .doOnSubscribe {
                Timber.i("Fetching balance for asset ${currency.displayTicker}")
                model.process(DashboardIntent.BalanceFetching(currency, true))
            }
            .doOnError { e ->
                Timber.e("Failed getting balance for ${currency.displayTicker}: $e")
                model.process(DashboardIntent.BalanceUpdateError(currency))
            }
            .doOnNext { accountBalance ->
                Timber.d("Got balance for ${currency.displayTicker}")
                model.process(DashboardIntent.BalanceUpdate(currency, accountBalance))
            }
            .doOnComplete { model.process(DashboardIntent.BalanceFetching(currency, false)) }
            .firstOrError()
            .map {
                it.total
            }

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
            is BrokerageCryptoAsset -> refreshPricesWith24HDelta(model, asset.currency as AssetInfo)
            is BrokearageFiatAsset,
            is DefiAsset -> refreshPrice(model, asset.currency)
        }.also {
            compositeDisposable += it
        }

    private fun refreshPrice(model: DashboardModel, currency: Currency): Disposable =
        exchangeRates.exchangeRateToUserFiat(currency).firstOrError()
            .map { price ->
                DashboardIntent.AssetPriceUpdate(currency, price)
            }
            .subscribeBy(
                onSuccess = { model.process(it) },
                onError = {
                    model.process(DashboardIntent.BalanceUpdateError(currency))
                }
            )

    fun refreshPriceHistory(model: DashboardModel, asset: AssetInfo): Disposable =
        if (asset.startDate != null) {
            coincore[asset].lastDayTrend().asObservable()
        } else {
            Observable.just(DataResource.Data(FLATLINE_CHART))
        }
            .subscribeBy(
                onNext = { dataResource ->
                    when (dataResource) {
                        is DataResource.Data -> {
                            model.process(DashboardIntent.PriceHistoryUpdate(asset, dataResource.data))
                        }
                        is DataResource.Error -> {
                            Timber.e(dataResource.error)
                        }
                        DataResource.Loading -> {
                        }
                    }
                }
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
        kycService.getHighestApprovedTierLevelLegacy()
            .flatMap {
                if (it == KycTier.GOLD) {
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
            coincore.allWallets().map { it.accounts }.map { it.filterIsInstance<FiatAccount>() }
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

                            val selectedAccount = fiatGroups.first {
                                it.currency.networkTicker == networkTicker
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
            linkedBanksFactory.eligibleBankPaymentMethods(sourceAccount.currency)
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
            Singles.zip(
                userIdentity.isCowboysUser(),
                cowboysFeatureFlag.enabled,
            ).flatMap { (isCowboysUser, cowboysFlagEnabled) ->
                if (isCowboysUser && cowboysFlagEnabled) {
                    Single.just(DashboardOnboardingState.Hidden)
                } else {
                    if (!walletMode.custodialEnabled) {
                        Single.just(DashboardOnboardingState.Hidden)
                    } else {
                        getDashboardOnboardingStepsUseCase(Unit).doOnSuccess { steps ->
                            val hasBoughtCrypto =
                                steps.find { it.step == DashboardOnboardingStep.BUY }?.isCompleted == true
                            if (hasBoughtCrypto) onboardingPrefs.isLandingCtaDismissed = true
                        }.map { steps ->
                            if (steps.any { !it.isCompleted }) {
                                DashboardOnboardingState.Visible(steps)
                            } else {
                                DashboardOnboardingState.Hidden
                            }
                        }
                    }
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

    fun markCowboysReferralCardAsDismissed() {
        cowboysPrefs.hasCowboysReferralBeenDismissed = true
    }

    fun checkCowboysFlowSteps(model: DashboardModel): Disposable =
        Singles.zip(
            userIdentity.isCowboysUser(),
            cowboysFeatureFlag.enabled,
        ).flatMap { (isCowboysUser, cowboysFlagEnabled) ->
            if (cowboysFlagEnabled && isCowboysUser) {
                checkEmailVerificationState()
            } else {
                Single.just(DashboardCowboysState.Hidden)
            }
        }.subscribeBy(
            onSuccess = { state ->
                model.process(DashboardIntent.UpdateCowboysViewState(state))
            },
            onError = {
                Timber.e("Error in cowboys state ${it.message}")
                model.process(DashboardIntent.UpdateCowboysViewState(DashboardCowboysState.Hidden))
            }
        )

    private fun checkEmailVerificationState(): Single<DashboardCowboysState> =
        settingsDataManager.getSettings().firstOrError().flatMap { userSettings ->
            if (!userSettings.isEmailVerified) {
                cowboysDataProvider.getWelcomeAnnouncement().flatMap {
                    Single.just(DashboardCowboysState.CowboyWelcomeCard(it))
                }
            } else {
                checkHighestApprovedKycTier()
            }
        }

    private fun checkHighestApprovedKycTier(): Single<DashboardCowboysState> =
        kycService.getHighestApprovedTierLevelLegacy().flatMap { highestApprovedKycTier ->
            when (highestApprovedKycTier) {
                KycTier.BRONZE -> cowboysDataProvider.getRaffleAnnouncement().flatMap {
                    Single.just(DashboardCowboysState.CowboyRaffleCard(it))
                }
                KycTier.SILVER -> cowboysDataProvider.getIdentityAnnouncement().flatMap {
                    Single.just(DashboardCowboysState.CowboyIdentityCard(it))
                }
                else -> getCowboysReferralInfo()
            }
        }

    private fun getCowboysReferralInfo(): Single<DashboardCowboysState> =
        if (cowboysPrefs.hasCowboysReferralBeenDismissed) {
            Single.just(DashboardCowboysState.Hidden)
        } else {
            Single.zip(
                getReferralData(),
                cowboysDataProvider.getReferFriendsAnnouncement()
            ) { referralInfo, cowboysData ->
                DashboardCowboysState.CowboyReferFriendsCard(
                    referralData = referralInfo,
                    cardInfo = cowboysData
                )
            }
        }

    private fun getReferralData(): Single<ReferralInfo> =
        rxSingle(Schedulers.io().asCoroutineDispatcher()) {
            referralService.fetchReferralData()
                .getOrDefault(ReferralInfo.NotAvailable)
        }.onErrorResumeWith { ReferralInfo.NotAvailable }

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

    fun checkReferralSuccess(model: DashboardModel): Disposable =
        Singles.zip(
            userIdentity.isCowboysUser(),
            cowboysFeatureFlag.enabled,
        ).flatMapCompletable { (isCowboysUser, cowboysFlagEnabled) ->
            if (isCowboysUser && cowboysFlagEnabled) {
                Completable.complete()
            } else {
                Completable.fromAction {
                    val title = referralPrefs.referralSuccessTitle
                    val body = referralPrefs.referralSuccessBody
                    if (title.isNotBlank() && body.isNotBlank()) {
                        model.process(DashboardIntent.ShowReferralSuccess(Pair(title, body)))
                    }
                }
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
