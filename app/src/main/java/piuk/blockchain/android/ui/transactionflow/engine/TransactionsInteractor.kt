package piuk.blockchain.android.ui.transactionflow.engine

import com.blockchain.coincore.AddressFactory
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.EarnRewardsAccount
import com.blockchain.coincore.ExchangeAccount
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.ReceiveAddress
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.SingleAccountList
import com.blockchain.coincore.TransactionProcessor
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.coincore.TxValidationFailure
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.fiat.LinkedBanksFactory
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.core.announcements.DismissRecorder
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import com.blockchain.data.asMaybe
import com.blockchain.data.asSingle
import com.blockchain.domain.fiatcurrencies.FiatCurrenciesService
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.PaymentMethodService
import com.blockchain.domain.paymentmethods.model.BankAuthDeepLinkState
import com.blockchain.domain.paymentmethods.model.BankAuthFlowState
import com.blockchain.domain.paymentmethods.model.BankPaymentApproval
import com.blockchain.domain.paymentmethods.model.DepositTerms
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.domain.paymentmethods.model.toPreferencesValue
import com.blockchain.domain.trade.TradeDataService
import com.blockchain.domain.trade.model.QuickFillRoundingData
import com.blockchain.earn.domain.service.ActiveRewardsService
import com.blockchain.earn.domain.service.StakingService
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.fiatActions.fiatactions.models.LinkablePaymentMethods
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.repositories.swap.CustodialRepository
import com.blockchain.preferences.BankLinkingPrefs
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.TransactionPrefs
import com.blockchain.utils.mapList
import com.blockchain.utils.rxSingleOutcome
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.CurrencyPair
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import info.blockchain.balance.asAssetInfoOrThrow
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.kotlin.zipWith
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.rx3.rxSingle
import piuk.blockchain.android.ui.transfer.AccountsSorting
import timber.log.Timber

class TransactionInteractor(
    private val coincore: Coincore,
    private val addressFactory: AddressFactory,
    private val custodialRepository: CustodialRepository,
    private val custodialWalletManager: CustodialWalletManager,
    private val bankService: BankService,
    private val paymentMethodService: PaymentMethodService,
    private val currencyPrefs: CurrencyPrefs,
    private val identity: UserIdentity,
    private val defaultAccountsSorting: AccountsSorting,
    private val swapSourceAccountsSorting: AccountsSorting,
    private val swapTargetAccountsSorting: AccountsSorting,
    private val linkedBanksFactory: LinkedBanksFactory,
    private val bankLinkingPrefs: BankLinkingPrefs,
    private val dismissRecorder: DismissRecorder,
    private val fiatCurrenciesService: FiatCurrenciesService,
    private val tradeDataService: TradeDataService,
    private val improvedPaymentUxFF: FeatureFlag,
    private val stakingService: StakingService,
    private val transactionPrefs: TransactionPrefs,
    private val activeRewardsService: ActiveRewardsService
) {
    private var transactionProcessor: TransactionProcessor? = null
    private val invalidate = PublishSubject.create<Unit>()

    fun invalidateTransaction(): Completable =
        Completable.fromAction {
            reset()
            transactionProcessor = null
        }

    fun validatePassword(password: String): Single<Boolean> =
        Single.just(coincore.validateSecondPassword(password))

    fun validateTargetAddress(address: String, asset: AssetInfo): Single<ReceiveAddress> =
        addressFactory.parse(address, asset)
            .switchIfEmpty(
                Single.error(
                    TxValidationFailure(ValidationState.INVALID_ADDRESS)
                )
            )

    fun initialiseTransaction(
        sourceAccount: BlockchainAccount,
        target: TransactionTarget,
        action: AssetAction
    ): Observable<PendingTx> =
        coincore.createTransactionProcessor(sourceAccount, target, action)
            .doOnSubscribe { Timber.d("!TRANSACTION!> SUBSCRIBE") }
            .doOnSuccess {
                if (transactionProcessor != null) {
                    throw IllegalStateException("TxProcessor double init")
                }
            }
            .doOnSuccess { transactionProcessor = it }
            .doOnError {
                Timber.e("!TRANSACTION!> error initialising $it")
            }.flatMapObservable {
                it.initialiseTx()
            }.takeUntil(invalidate)

    val canTransactFiat: Boolean
        get() = transactionProcessor?.canTransactFiat ?: throw IllegalStateException("TxProcessor not initialised")

    fun updateTransactionAmount(amount: Money): Completable =
        transactionProcessor?.updateAmount(amount) ?: throw IllegalStateException("TxProcessor not initialised")

    fun updateTransactionFees(feeLevel: FeeLevel, customFeeAmount: Long?): Completable =
        transactionProcessor?.updateFeeLevel(
            level = feeLevel,
            customFeeAmount = customFeeAmount
        ) ?: throw IllegalStateException("TxProcessor not initialised")

    fun getTargetAccounts(sourceAccount: BlockchainAccount, action: AssetAction): Single<SingleAccountList> {
        val accounts = when (action) {
            AssetAction.Swap -> swapTargets(sourceAccount as CryptoAccount)
            AssetAction.Sell -> sellTargets(sourceAccount as CryptoAccount)
            AssetAction.Send -> coincore[(sourceAccount as SingleAccount).currency]
                .transactionTargets(sourceAccount)

            AssetAction.FiatDeposit -> linkedBanksFactory.getNonWireTransferBanks().mapList { it }
            AssetAction.FiatWithdraw -> linkedBanksFactory.getAllLinkedBanks().mapList { it }
            else -> coincore.getTransactionTargets(sourceAccount as CryptoAccount, action)
        }
        return accounts.flatMap {
            defaultAccountsSorting.sorter().invoke(it)
        }
    }

    private fun sellTargets(sourceAccount: CryptoAccount): Single<List<SingleAccount>> {
        val availableFiats =
            rxSingle { custodialWalletManager.getSupportedFundsFiats(currencyPrefs.selectedFiatCurrency).first() }
        val apiPairs = Single.zip(
            custodialWalletManager.getSupportedBuySellCryptoCurrencies(),
            availableFiats
        ) { supportedPairs, fiats ->
            supportedPairs.filter { fiats.contains(it.destination) }
        }

        return Singles.zip(
            coincore.getTransactionTargets(sourceAccount, AssetAction.Sell),
            apiPairs
        ).map { (accountList, pairs) ->
            val fiatAccounts = accountList.filterIsInstance(FiatAccount::class.java)
                .filter { account ->
                    pairs.any { it.source == sourceAccount.currency && account.currency == it.destination }
                }
            val selectedTradingCurrency = fiatCurrenciesService.selectedTradingCurrency
            val selectedTradingAccount =
                fiatAccounts.find { it.currency == selectedTradingCurrency }

            if (selectedTradingAccount != null) {
                listOf(selectedTradingAccount)
            } else {
                fiatAccounts
            }
        }
    }

    @Deprecated("use SwapService.swapTargets")
    private fun swapTargets(sourceAccount: CryptoAccount): Single<List<SingleAccount>> {
        return custodialRepository.getSwapAvailablePairs().flatMap { pairs ->
            val targetCurrencies =
                pairs.filter { it.source.networkTicker == sourceAccount.currency.networkTicker }.map { it.destination }
            val assets = targetCurrencies.map { coincore[it] }
            Single.just(assets).flattenAsObservable { it }.flatMapSingle { asset ->
                asset.accountGroup(AssetFilter.All).map { it.accounts }.switchIfEmpty(Single.just(emptyList()))
            }.reduce { t1, t2 ->
                t1 + t2
            }.switchIfEmpty(Single.just(emptyList()))
                .map {
                    it.filterIsInstance<CryptoAccount>()
                        .filterNot { account ->
                            account is EarnRewardsAccount ||
                                account is ExchangeAccount
                        }
                        .filterNot { account -> account.currency.networkTicker == sourceAccount.currency.networkTicker }
                        .filter { cryptoAccount ->
                            sourceAccount.isTargetAvailableForSwap(
                                target = cryptoAccount
                            )
                        }
                }
        }.flatMap { list ->
            swapTargetAccountsSorting.sorter().invoke(list)
        }
    }

    /**
     * When wallet is in Universal mode, you can swap from Trading to Trading, from PK to PK and from PK to Trading
     * In any other case, swap is only allowed to same Type accounts
     */
    private fun SingleAccount.isTargetAvailableForSwap(
        target: CryptoAccount
    ): Boolean =
        if (this is CustodialTradingAccount) target is CustodialTradingAccount else true

    fun getAvailableSourceAccounts(
        action: AssetAction,
        targetAccount: TransactionTarget,
        showPkwOnTradingMode: Boolean
    ): Single<SingleAccountList> =
        when (action) {
            AssetAction.Swap -> {
                getAvailableSwapAccounts().map { it as SingleAccountList }
            }

            AssetAction.InterestDeposit -> {
                require(targetAccount is EarnRewardsAccount.Interest)
                require(targetAccount is CryptoAccount)
                coincore.walletsWithAction(
                    action = action,
                    filter = AssetFilter.All,
                    tickers = setOf(targetAccount.currency),
                    sorter = defaultAccountsSorting.sorter()
                ).map {
                    it.filter { acc ->
                        acc is CustodialTradingAccount ||
                            (acc is NonCustodialAccount && showPkwOnTradingMode)
                    }
                }
            }

            AssetAction.StakingDeposit -> {
                require(targetAccount is EarnRewardsAccount.Staking)
                require(targetAccount is CryptoAccount)
                coincore.walletsWithAction(
                    action = action,
                    filter = AssetFilter.All,
                    tickers = setOf(targetAccount.currency),
                    sorter = defaultAccountsSorting.sorter()
                ).map {
                    it.filter { acc ->
                        acc is CustodialTradingAccount ||
                            (acc is NonCustodialAccount && showPkwOnTradingMode)
                    }
                }
            }

            AssetAction.ActiveRewardsDeposit -> {
                require(targetAccount is EarnRewardsAccount.Active)
                require(targetAccount is CryptoAccount)
                coincore.walletsWithAction(
                    action = action,
                    filter = AssetFilter.All,
                    tickers = setOf(targetAccount.currency),
                    sorter = defaultAccountsSorting.sorter()
                ).map {
                    it.filter { acc ->
                        acc is CustodialTradingAccount ||
                            (acc is NonCustodialAccount && showPkwOnTradingMode)
                    }
                }
            }

            AssetAction.FiatDeposit -> {
                linkedBanksFactory.getNonWireTransferBanks().map { it }
            }

            AssetAction.Sell -> sellSourceAccounts()
            else -> throw IllegalStateException("Source account should be preselected for action $action")
        }

    fun shouldShowPkwOnTradingMode(): Boolean =
        transactionPrefs.showPkwAccountsOnTradingMode

    fun updatePkwFilterState(showPkwOnTradingMode: Boolean) {
        transactionPrefs.showPkwAccountsOnTradingMode = showPkwOnTradingMode
    }

    private fun getAvailableSwapAccounts() = coincore.walletsWithAction(
        action = AssetAction.Swap,
        sorter = swapSourceAccountsSorting.sorter()
    ).zipWith(
        custodialRepository.getSwapAvailablePairs()
    ).map { (accounts, pairs) ->
        accounts.filter { account ->
            (account as? CryptoAccount)?.isAvailableToSwapFrom(pairs) ?: false
        }
    }.map {
        it.map { account -> account as CryptoAccount }
    }

    private fun sellSourceAccounts(): Single<List<SingleAccount>> {
        return supportedCryptoCurrencies().zipWith(
            coincore.walletsWithAction(action = AssetAction.Sell, sorter = defaultAccountsSorting.sorter())
        ).map { (assets, accounts) ->
            accounts.filterIsInstance<CryptoAccount>().filter { account ->
                account.currency.networkTicker in assets.map { it.networkTicker }
            }
        }
    }

    private fun supportedCryptoCurrencies(): Single<List<AssetInfo>> {
        val availableFiats =
            rxSingle { custodialWalletManager.getSupportedFundsFiats(currencyPrefs.selectedFiatCurrency).first() }
        return Single.zip(
            custodialWalletManager.getSupportedBuySellCryptoCurrencies(),
            availableFiats
        ) { supportedPairs, fiats ->
            supportedPairs
                .filter { fiats.contains(it.destination) }
                .map { it.source.asAssetInfoOrThrow() }
        }
    }

    fun verifyAndExecute(secondPassword: String): Completable =
        transactionProcessor?.execute(secondPassword) ?: throw IllegalStateException("TxProcessor not initialised")

    fun cancelTransaction(): Completable =
        transactionProcessor?.cancel() ?: throw IllegalStateException("TxProcessor not initialised")

    fun modifyOptionValue(newConfirmation: TxConfirmationValue): Completable =
        transactionProcessor?.setOption(newConfirmation) ?: throw IllegalStateException("TxProcessor not initialised")

    fun startFiatRateFetch(): Observable<ExchangeRate> =
        transactionProcessor?.userExchangeRate()?.takeUntil(invalidate) ?: throw IllegalStateException(
            "TxProcessor not initialised"
        )

    fun startTargetRateFetch(): Observable<ExchangeRate> =
        transactionProcessor?.targetExchangeRate()?.takeUntil(invalidate) ?: throw IllegalStateException(
            "TxProcessor not initialised"
        )

    fun startConfirmationRateFetch(): Observable<ExchangeRate> =
        transactionProcessor?.confirmationExchangeRate()?.takeUntil(invalidate) ?: throw IllegalStateException(
            "TxProcessor not initialised"
        )

    fun validateTransaction(): Completable =
        transactionProcessor?.validateAll() ?: throw IllegalStateException("TxProcessor not initialised")

    fun reset() {
        invalidate.onNext(Unit)
        transactionProcessor?.reset() ?: Timber.i("TxProcessor is not initialised yet")
    }

    fun linkABank(selectedFiat: FiatCurrency): Single<LinkBankTransfer> =
        bankService.linkBank(selectedFiat)

    fun updateFiatDepositState(bankPaymentData: BankPaymentApproval) {
        bankLinkingPrefs.setBankLinkingState(
            BankAuthDeepLinkState(
                bankAuthFlow = BankAuthFlowState.BANK_APPROVAL_PENDING,
                bankPaymentData = bankPaymentData
            ).toPreferencesValue()
        )

        val sanitisedUrl = bankPaymentData.linkedBank.callbackPath.removePrefix("nabu-gateway/")
        bankLinkingPrefs.setDynamicOneTimeTokenUrl(sanitisedUrl)
    }

    fun loadWithdrawalLocks(model: TransactionModel, available: Money): Disposable =
        coincore.getWithdrawalLocks(showLocksInFiat(available)).asMaybe().subscribeBy(
            onSuccess = { locks ->
                model.process(TransactionIntent.FundsLocksLoaded(locks))
            },
            onError = {
                Timber.e(it)
            }
        )

    private fun showLocksInFiat(available: Money): Currency {
        return if (available is FiatValue) {
            available.currency
        } else {
            currencyPrefs.selectedFiatCurrency
        }
    }

    fun updateFiatDepositOptions(fiatCurrency: FiatCurrency): Single<TransactionIntent> {
        return paymentMethodService.getEligiblePaymentMethodTypes(fiatCurrency).map { available ->
            val availableBankPaymentMethodTypes = available.filter {
                it.type == PaymentMethodType.BANK_TRANSFER ||
                    it.type == PaymentMethodType.BANK_ACCOUNT
            }.filter { it.currency == fiatCurrency }.map { it.type }.sortedBy { it.ordinal }

            when {
                availableBankPaymentMethodTypes.size > 1 -> {
                    TransactionIntent.FiatDepositOptionSelected(
                        DepositOptionsState.ShowBottomSheet(
                            LinkablePaymentMethods(fiatCurrency, availableBankPaymentMethodTypes)
                        )
                    )
                }

                availableBankPaymentMethodTypes.size == 1 -> {
                    when {
                        availableBankPaymentMethodTypes.first() == PaymentMethodType.BANK_TRANSFER -> {
                            TransactionIntent.FiatDepositOptionSelected(DepositOptionsState.LaunchLinkBank)
                        }

                        availableBankPaymentMethodTypes.first() == PaymentMethodType.BANK_ACCOUNT -> {
                            TransactionIntent.FiatDepositOptionSelected(
                                DepositOptionsState.LaunchWireTransfer(fiatCurrency)
                            )
                        }

                        else -> {
                            TransactionIntent.FiatDepositOptionSelected(DepositOptionsState.None)
                        }
                    }
                }

                else -> {
                    TransactionIntent.FiatDepositOptionSelected(DepositOptionsState.None)
                }
            }
        }
    }

    fun loadSendToDomainAnnouncementPref(prefsKey: String): Single<Boolean> =
        Single.just(!dismissRecorder.isDismissed(prefsKey))

    fun dismissSendToDomainAnnouncementPref(prefsKey: String): Single<Boolean> =
        Single.fromCallable {
            dismissRecorder.dismissForever(prefsKey)
            dismissRecorder.isDismissed(prefsKey)
        }

    fun userAccessForFeature(feature: Feature): Single<FeatureAccess> =
        identity.userAccessForFeature(feature)

    fun checkShouldShowRewardsInterstitial(
        sourceAccount: BlockchainAccount,
        asset: AssetInfo,
        action: AssetAction
    ): Single<FeatureAccess> =
        if (sourceAccount !is NullCryptoAccount) {
            Single.just(FeatureAccess.Granted())
        } else {
            when (action) {
                AssetAction.StakingDeposit -> {
                    Single.zip(
                        stakingService.getLimitsForAsset(asset).asSingle(),
                        stakingService.getBalanceForAsset(asset).asSingle(),
                        identity.userAccessForFeature(Feature.DepositStaking)
                    ) { limits, accountBalance, userAccess ->
                        if (userAccess is FeatureAccess.Granted) {
                            if (limits.withdrawalsDisabled) {
                                FeatureAccess.Blocked(BlockedReason.NotEligible(null))
                            } else if (accountBalance.totalBalance.isZero) {
                                FeatureAccess.Blocked(
                                    BlockedReason.ShouldAcknowledgeStakingWithdrawal(
                                        assetIconUrl = asset.logo,
                                        unbondingDays = limits.unbondingDays
                                    )
                                )
                            } else FeatureAccess.Granted()
                        } else {
                            userAccess
                        }
                    }
                }

                AssetAction.ActiveRewardsDeposit -> {
                    activeRewardsService.getBalanceForAsset(
                        asset, FreshnessStrategy.Cached(RefreshStrategy.RefreshIfStale)
                    ).asSingle().map { accountBalance ->
                        if (accountBalance.totalBalance.isZero) {
                            FeatureAccess.Blocked(
                                BlockedReason.ShouldAcknowledgeActiveRewardsWithdrawalWarning
                            )
                        } else FeatureAccess.Granted()
                    }
                }

                else -> Single.just(FeatureAccess.Granted())
            }
        }

    fun getRewardsWithdrawalUnbondingDays(asset: AssetInfo, account: EarnRewardsAccount): Single<Int> =
        when (account) {
            is EarnRewardsAccount.Staking -> {
                stakingService.getLimitsForAsset(asset).asSingle().map { it.unbondingDays }
            }

            is EarnRewardsAccount.Active -> {
                activeRewardsService.getLimitsForAsset(asset).asSingle().map { it.unbondingDays }
            }

            else -> {
                Single.just(2)
            }
        }

    fun updateStakingExplainerAcknowledged(networkTicker: String) {}

    fun getRoundingDataForAction(action: AssetAction): Single<List<QuickFillRoundingData>> =
        when (action) {
            AssetAction.Swap -> rxSingleOutcome { tradeDataService.getQuickFillRoundingForSwap() }
            AssetAction.Sell -> rxSingleOutcome { tradeDataService.getQuickFillRoundingForSell() }
            else -> Single.just(emptyList())
        }

    fun isImprovedPaymentUxFFEnabled() = improvedPaymentUxFF.enabled

    fun getDepositTerms(paymentMethodId: String, amount: Money): Single<DepositTerms> =
        rxSingleOutcome { bankService.getDepositTerms(paymentMethodId, amount) }
}

private fun CryptoAccount.isAvailableToSwapFrom(pairs: List<CurrencyPair>): Boolean =
    pairs.any { it.source == this.currency }
