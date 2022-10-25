package piuk.blockchain.android.ui.transactionflow.engine

import com.blockchain.banking.BankPaymentApproval
import com.blockchain.coincore.AddressFactory
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.Coincore
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.InterestAccount
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
import com.blockchain.domain.fiatcurrencies.FiatCurrenciesService
import com.blockchain.domain.paymentmethods.BankService
import com.blockchain.domain.paymentmethods.PaymentMethodService
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.CurrencyPair
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.repositories.swap.CustodialRepository
import com.blockchain.preferences.BankLinkingPrefs
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.LocalSettingsPrefs
import com.blockchain.utils.mapList
import com.blockchain.utils.zipObservables
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
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
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.linkbank.BankAuthDeepLinkState
import piuk.blockchain.android.ui.linkbank.BankAuthFlowState
import piuk.blockchain.android.ui.linkbank.toPreferencesValue
import piuk.blockchain.android.ui.settings.LinkablePaymentMethods
import piuk.blockchain.android.ui.transactionflow.engine.domain.QuickFillRoundingService
import piuk.blockchain.android.ui.transactionflow.engine.domain.model.QuickFillRoundingData
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
    private val swapSellQuickFillFF: FeatureFlag,
    private val quickFillRoundingService: QuickFillRoundingService,
    private val hideDustFF: FeatureFlag,
    private val localSettingsPrefs: LocalSettingsPrefs
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
        action: AssetAction,
    ): Observable<PendingTx> =
        coincore.createTransactionProcessor(sourceAccount, target, action)
            .doOnSubscribe { Timber.d("!TRANSACTION!> SUBSCRIBE") }
            .doOnSuccess {
                if (transactionProcessor != null)
                    throw IllegalStateException("TxProcessor double init")
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

    fun getTargetAccounts(sourceAccount: BlockchainAccount, action: AssetAction): Single<SingleAccountList> =
        when (action) {
            AssetAction.Swap -> swapTargets(sourceAccount as CryptoAccount)
            AssetAction.Sell -> sellTargets(sourceAccount as CryptoAccount)
            AssetAction.FiatDeposit -> linkedBanksFactory.getNonWireTransferBanks().mapList { it }
            AssetAction.FiatWithdraw -> linkedBanksFactory.getAllLinkedBanks().mapList { it }
            else -> coincore.getTransactionTargets(sourceAccount as CryptoAccount, action)
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

            if (selectedTradingAccount != null) listOf(selectedTradingAccount)
            else fiatAccounts
        }
    }

    private fun swapTargets(sourceAccount: CryptoAccount): Single<List<SingleAccount>> =
        Single.zip(
            coincore.getTransactionTargets(sourceAccount, AssetAction.Swap),
            custodialRepository.getSwapAvailablePairs()
        ) { accountList, pairs ->
            accountList.filterIsInstance(CryptoAccount::class.java)
                .filter { account ->
                    pairs.any { it.source == sourceAccount.currency && account.currency == it.destination }
                }
        }.flatMap { list ->
            swapTargetAccountsSorting.sorter().invoke(list)
        }

    fun getAvailableSourceAccounts(
        action: AssetAction,
        targetAccount: TransactionTarget,
    ): Single<SingleAccountList> =
        when (action) {
            AssetAction.Swap -> {
                hideDustFF.enabled.flatMap { flagEnabled ->
                    getAvailableSwapAccounts().flatMap { accountList ->
                        if (flagEnabled && localSettingsPrefs.hideSmallBalancesEnabled) {
                            filterDustBalances(accountList)
                        } else {
                            Single.just(accountList)
                        }
                    }
                }
            }
            AssetAction.InterestDeposit -> {
                require(targetAccount is InterestAccount)
                require(targetAccount is CryptoAccount)
                coincore.walletsWithActions(actions = setOf(action), sorter = defaultAccountsSorting.sorter()).map {
                    it.filter { acc ->
                        acc is CryptoAccount &&
                            acc.currency == targetAccount.currency &&
                            acc != targetAccount &&
                            acc.isFunded
                    }
                }
            }
            AssetAction.FiatDeposit -> {
                linkedBanksFactory.getNonWireTransferBanks().map { it }
            }
            AssetAction.Sell -> sellSourceAccounts()
            else -> throw IllegalStateException("Source account should be preselected for action $action")
        }

    private fun filterDustBalances(accountList: List<CryptoAccount>) =
        accountList.map { account ->
            account.balanceRx
        }.zipObservables().map {
            accountList.mapIndexedNotNull { index, singleAccount ->
                if (!it[index].totalFiat.isDust()) {
                    singleAccount
                } else {
                    null
                }
            }
        }.firstOrError()

    private fun getAvailableSwapAccounts() = coincore.walletsWithActions(
        actions = setOf(AssetAction.Swap),
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
            coincore.walletsWithActions(actions = setOf(AssetAction.Sell), sorter = defaultAccountsSorting.sorter())
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
            custodialWalletManager.getSupportedBuySellCryptoCurrencies(), availableFiats
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
        coincore.getWithdrawalLocks(showLocksInFiat(available)).subscribeBy(
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

    fun userAccessForFeature(feature: Feature): Single<FeatureAccess> = identity.userAccessForFeature(feature)

    fun isSwapSellQuickFillFFEnabled() = swapSellQuickFillFF.enabled

    fun getRoundingDataForAction(action: AssetAction): Single<List<QuickFillRoundingData>> =
        quickFillRoundingService.getQuickFillRoundingForAction(action)
}

private fun CryptoAccount.isAvailableToSwapFrom(pairs: List<CurrencyPair>): Boolean =
    pairs.any { it.source == this.currency }
