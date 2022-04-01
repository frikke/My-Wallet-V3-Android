package piuk.blockchain.android.ui.transactionflow.engine

import com.blockchain.banking.BankPaymentApproval
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.CryptoTarget
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.InterestAccount
import com.blockchain.coincore.NeedsApprovalException
import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.NullAddress
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.coincore.PendingTx
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.TradingAccount
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.coincore.TxValidationFailure
import com.blockchain.coincore.ValidationState
import com.blockchain.coincore.fiat.FiatCustodialAccount
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.commonarch.presentation.mvi.MviModel
import com.blockchain.commonarch.presentation.mvi.MviState
import com.blockchain.core.eligibility.models.TransactionsLimit
import com.blockchain.core.limits.TxLimit
import com.blockchain.core.limits.TxLimits
import com.blockchain.core.payments.model.FundsLocks
import com.blockchain.core.payments.model.LinkBankTransfer
import com.blockchain.core.price.ExchangeRate
import com.blockchain.core.price.canConvert
import com.blockchain.enviroment.EnvironmentConfig
import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.Feature
import com.blockchain.nabu.FeatureAccess
import com.blockchain.remoteconfig.FeatureFlag
import info.blockchain.balance.AssetCategory
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Currency
import info.blockchain.balance.CurrencyType
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import java.util.Stack
import piuk.blockchain.android.ui.settings.v2.LinkablePaymentMethods
import piuk.blockchain.android.ui.transactionflow.flow.getLabelForDomain
import timber.log.Timber

enum class TransactionStep(val addToBackStack: Boolean = false) {
    ZERO,
    NOT_ELIGIBLE,
    ENTER_PASSWORD,
    SELECT_SOURCE(true),
    ENTER_ADDRESS(true),
    SELECT_TARGET_ACCOUNT(true),
    ENTER_AMOUNT(true),
    CONFIRM_DETAIL,
    IN_PROGRESS,
    CLOSED
}

enum class TransactionErrorState {
    NONE,
    INVALID_PASSWORD,
    INVALID_ADDRESS,
    INVALID_DOMAIN,
    ADDRESS_IS_CONTRACT,
    INSUFFICIENT_FUNDS,
    INVALID_AMOUNT,
    BELOW_MIN_LIMIT,
    BELOW_MIN_PAYMENT_METHOD_LIMIT,
    ABOVE_MAX_PAYMENT_METHOD_LIMIT,
    PENDING_ORDERS_LIMIT_REACHED,
    OVER_SILVER_TIER_LIMIT,
    OVER_GOLD_TIER_LIMIT,
    NOT_ENOUGH_GAS,
    UNEXPECTED_ERROR,
    TRANSACTION_IN_FLIGHT,
    TX_OPTION_INVALID,
    UNKNOWN_ERROR
}

sealed class BankLinkingState {
    object NotStarted : BankLinkingState()
    class Success(val bankTransferInfo: LinkBankTransfer) : BankLinkingState()
    class Error(val e: Throwable) : BankLinkingState()
}

sealed class DepositOptionsState {
    object None : DepositOptionsState()
    data class ShowBottomSheet(val linkablePaymentMethods: LinkablePaymentMethods) : DepositOptionsState()
    data class LaunchWireTransfer(val fiatCurrency: FiatCurrency) : DepositOptionsState()
    object LaunchLinkBank : DepositOptionsState()
    data class Error(val e: Throwable) : DepositOptionsState()
}

sealed class TxExecutionStatus {
    object NotStarted : TxExecutionStatus()
    object InProgress : TxExecutionStatus()
    object Completed : TxExecutionStatus()
    object Cancelled : TxExecutionStatus()
    class ApprovalRequired(val approvalData: BankPaymentApproval) : TxExecutionStatus()
    data class Error(val exception: Throwable) : TxExecutionStatus()
}

fun SingleAccount.getZeroAmountForAccount() =
    Money.zero(this.currency)

data class TransactionState(
    override val action: AssetAction = AssetAction.Send,
    val currentStep: TransactionStep = TransactionStep.ZERO,
    val sendingAccount: SingleAccount = NullCryptoAccount(),
    val selectedTarget: TransactionTarget = NullAddress,
    override val fiatRate: ExchangeRate? = null,
    val targetRate: ExchangeRate? = null,
    val passwordRequired: Boolean = false,
    val secondPassword: String = "",
    val nextEnabled: Boolean = false,
    val setMax: Boolean = false,
    override val errorState: TransactionErrorState = TransactionErrorState.NONE,
    val pendingTx: PendingTx? = null,
    val allowFiatInput: Boolean = false,
    val executionStatus: TxExecutionStatus = TxExecutionStatus.NotStarted,
    val stepsBackStack: Stack<TransactionStep> = Stack(),
    val availableTargets: List<TransactionTarget> = emptyList(),
    val currencyType: CurrencyType? = null,
    val availableSources: List<BlockchainAccount> = emptyList(),
    val linkBankState: BankLinkingState = BankLinkingState.NotStarted,
    val depositOptionsState: DepositOptionsState = DepositOptionsState.None,
    val locks: FundsLocks? = null,
    val shouldShowSendToDomainBanner: Boolean = false,
    override val transactionsLimit: TransactionsLimit? = null
) : MviState, TransactionFlowStateInfo {

    // workaround for using engine without cryptocurrency source
    override val sendingAsset: Currency
        get() = sendingAccount.currency

    override val receivingAsset: Currency
        get() = when (selectedTarget) {
            is SingleAccount -> selectedTarget.currency
            is CryptoTarget -> selectedTarget.asset
            else -> throw IllegalStateException(
                "Missing receiving currency"
            )
        }

    override val limits: TxLimits
        get() = pendingTx?.limits ?: throw IllegalStateException("Limits are not define")

    override val sourceAccountType: AssetCategory
        get() = when (sendingAccount) {
            is TradingAccount -> AssetCategory.CUSTODIAL
            is InterestAccount -> AssetCategory.CUSTODIAL
            is CryptoNonCustodialAccount -> AssetCategory.NON_CUSTODIAL
            else -> throw IllegalStateException("$sendingAccount not supported")
        }

    override val amount: Money
        get() = pendingTx?.amount ?: sendingAccount.getZeroAmountForAccount()

    override val availableBalance: Money
        get() = pendingTx?.availableBalance ?: sendingAccount.getZeroAmountForAccount()

    val canGoBack: Boolean
        get() = stepsBackStack.isNotEmpty()

    val targetCount: Int
        get() = availableTargets.size

    val maxSpendable: Money
        get() {
            return pendingTx?.let {
                val available = availableToAmountCurrency(it.availableBalance, amount)
                Money.min(
                    available,
                    (it.limits?.max as? TxLimit.Limited)?.amount ?: available
                )
            } ?: sendingAccount.getZeroAmountForAccount()
        }

    val selectedTargetLabel: String
        get() = when {
            selectedTarget is CryptoAddress && selectedTarget.isDomain -> selectedTarget.getLabelForDomain()
            else -> selectedTarget.label
        }

    private fun availableToAmountCurrency(available: Money, amount: Money): Money =
        when (amount) {
            is FiatValue -> fiatRate?.convert(available) ?: Money.zero(amount.currency)
            is CryptoValue -> available
            else -> throw IllegalStateException("Unknown money type")
        }

    // If the current amount is the amount that was specified in a CryptoAddress target, then return that amount.
    // This is a hack to allow the enter amount screen to update to the initial amount, if one is specified,
    // without getting stuck in an update loop
    fun initialAmountToSet(): Money? {
        return (selectedTarget as? CryptoAddress)?.amount?.let { amount ->
            if (amount.isPositive) {
                amount
            } else {
                null
            }
        }
    }

    fun availableBalanceInFiat(availableBalance: Money, fiatRate: ExchangeRate?): Money {
        return if (availableBalance is CryptoValue &&
            fiatRate != null &&
            fiatRate.canConvert(availableBalance)
        ) {
            fiatRate.convert(availableBalance)
        } else {
            availableBalance
        }
    }
}

class TransactionModel(
    initialState: TransactionState,
    mainScheduler: Scheduler,
    private val interactor: TransactionInteractor,
    private val errorLogger: TxFlowErrorReporting,
    private val entitySwitchSilverEligibilityFeatureFlag: FeatureFlag,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger
) : MviModel<TransactionState, TransactionIntent>(
    initialState,
    mainScheduler,
    environmentConfig,
    crashLogger
) {

    override fun performAction(previousState: TransactionState, intent: TransactionIntent): Disposable? {
        Timber.v("!TRANSACTION!> Transaction Model: performAction: %s", intent.javaClass.simpleName)

        return when (intent) {
            is TransactionIntent.InitialiseWithSourceAccount -> processAccountsListUpdate(
                previousState,
                intent.fromAccount,
                intent.action
            )
            is TransactionIntent.InitialiseWithNoSourceOrTargetAccount -> processSourceAccountsListUpdate(
                intent.action, NullAddress
            )
            is TransactionIntent.InitialiseWithTargetAndNoSource -> processSourceAccountsListUpdate(
                intent.action, intent.target
            )
            is TransactionIntent.ReInitialiseWithTargetAndNoSource -> processSourceAccountsListUpdate(
                intent.action, intent.target, true
            )
            is TransactionIntent.InitialiseTransaction -> initialiseTransaction(
                intent.sourceAccount,
                intent.amount,
                intent.transactionTarget,
                intent.action,
                intent.eligibility
            )
            is TransactionIntent.ValidatePassword -> processPasswordValidation(intent.password)
            is TransactionIntent.UpdatePasswordIsValidated -> null
            is TransactionIntent.UpdatePasswordNotValidated -> null
            is TransactionIntent.PrepareTransaction -> null
            is TransactionIntent.AvailableSourceAccountsListUpdated -> null
            is TransactionIntent.SourceAccountSelected -> processAccountsListUpdate(
                previousState,
                intent.sourceAccount,
                previousState.action
            )
            is TransactionIntent.ExecuteTransaction -> processExecuteTransaction(previousState.secondPassword)
            is TransactionIntent.ValidateInputTargetAddress ->
                processValidateAddress(intent.targetAddress, intent.expectedCrypto)
            is TransactionIntent.CancelTransaction -> processCancelTransaction()
            is TransactionIntent.TargetAddressValidated -> null
            is TransactionIntent.TargetAddressOrDomainInvalid -> null
            is TransactionIntent.InitialiseWithSourceAndTargetAccount -> {
                processTargetSelectionConfirmed(
                    sourceAccount = intent.fromAccount,
                    amount = getInitialAmountFromTarget(intent),
                    transactionTarget = intent.target,
                    action = intent.action
                )
            }
            is TransactionIntent.TargetSelected ->
                processTargetSelectionConfirmed(
                    sourceAccount = previousState.sendingAccount,
                    amount = previousState.amount,
                    transactionTarget = previousState.selectedTarget,
                    action = previousState.action
                )
            is TransactionIntent.TargetSelectionUpdated -> null
            is TransactionIntent.InitialiseWithSourceAndPreferredTarget ->
                processAccountsListUpdate(previousState, intent.fromAccount, intent.action)
            is TransactionIntent.PendingTransactionStarted -> null
            is TransactionIntent.TargetAccountSelected -> null
            is TransactionIntent.FatalTransactionError -> null
            is TransactionIntent.AmountChanged -> processAmountChanged(intent.amount)
            is TransactionIntent.ModifyTxOption -> processModifyTxOptionRequest(intent.confirmation)
            is TransactionIntent.PendingTxUpdated -> null
            is TransactionIntent.DisplayModeChanged -> null
            is TransactionIntent.UpdateTransactionComplete -> null
            is TransactionIntent.ReturnToPreviousStep -> null
            is TransactionIntent.ShowTargetSelection -> null
            is TransactionIntent.FetchFiatRates -> processGetFiatRate()
            is TransactionIntent.FetchTargetRates -> processGetTargetRate()
            is TransactionIntent.FiatRateUpdated -> null
            is TransactionIntent.CryptoRateUpdated -> null
            is TransactionIntent.ValidateTransaction -> processValidateTransaction()
            is TransactionIntent.EnteredAddressReset -> null
            is TransactionIntent.AvailableAccountsListUpdated -> null
            is TransactionIntent.UpdateTransactionCancelled -> null
            is TransactionIntent.ShowMoreAccounts -> null
            is TransactionIntent.UseMaxSpendable -> null
            is TransactionIntent.SetFeeLevel -> processSetFeeLevel(intent)
            is TransactionIntent.InvalidateTransaction -> processInvalidateTransaction()
            is TransactionIntent.InvalidateTransactionKeepingTarget -> processInvalidationAndNavigate(previousState)
            is TransactionIntent.ResetFlow -> {
                interactor.reset()
                null
            }
            is TransactionIntent.RefreshSourceAccounts -> processSourceAccountsListUpdate(
                previousState.action, previousState.selectedTarget
            )
            is TransactionIntent.NavigateBackFromEnterAmount ->
                processTransactionInvalidation(previousState.action)
            is TransactionIntent.StartLinkABank -> processLinkABank(previousState)
            is TransactionIntent.LoadFundsLocked -> interactor.loadWithdrawalLocks(
                model = this,
                available = previousState.availableBalance
            )
            TransactionIntent.CheckAvailableOptionsForFiatDeposit -> processFiatDepositOptions(previousState)
            is TransactionIntent.LoadSendToDomainBannerPref -> processLoadSendToDomainPrefs(intent.prefsKey)
            is TransactionIntent.DismissSendToDomainBanner -> processDismissSendToDomainPrefs(intent.prefsKey)
            is TransactionIntent.LinkBankInfoSuccess,
            is TransactionIntent.LinkBankFailed,
            is TransactionIntent.ClearBackStack,
            is TransactionIntent.ApprovalRequired,
            is TransactionIntent.ClearSelectedTarget,
            TransactionIntent.TransactionApprovalDenied,
            TransactionIntent.ApprovalTriggered,
            is TransactionIntent.SendToDomainPrefLoaded,
            is TransactionIntent.FundsLocksLoaded,
            is TransactionIntent.ShowKycUpgradeNow,
            is TransactionIntent.FiatDepositOptionSelected -> null
        }
    }

    private fun getInitialAmountFromTarget(
        intent: TransactionIntent.InitialiseWithSourceAndTargetAccount
    ): Money {
        val amountSource = intent.target as? CryptoAddress
        return if (amountSource != null) {
            amountSource.amount ?: intent.fromAccount.getZeroAmountForAccount()
        } else {
            intent.fromAccount.getZeroAmountForAccount()
        }
    }

    private fun processTransactionInvalidation(assetAction: AssetAction): Disposable? {
        process(
            when (assetAction) {
                AssetAction.FiatDeposit -> TransactionIntent.InvalidateTransactionKeepingTarget
                else -> TransactionIntent.InvalidateTransaction
            }
        )
        return null
    }

    private fun processLinkABank(previousState: TransactionState): Disposable =
        interactor.linkABank((previousState.selectedTarget as FiatAccount).currency)
            .subscribeBy(
                onSuccess = { bankTransfer ->
                    process(TransactionIntent.LinkBankInfoSuccess(bankTransfer))
                },
                onError = {
                    process(TransactionIntent.LinkBankFailed(it))
                }
            )

    private fun processAccountsListUpdate(
        previousState: TransactionState,
        fromAccount: BlockchainAccount,
        action: AssetAction
    ): Disposable? =
        if (previousState.selectedTarget is NullAddress) {
            interactor.getTargetAccounts(fromAccount, action).subscribeBy(
                onSuccess = {
                    process(TransactionIntent.AvailableAccountsListUpdated(it))
                },
                onError = {
                    Timber.e("Error getting target accounts $it")
                }
            )
        } else {
            process(TransactionIntent.TargetSelected)
            null
        }

    private fun processSourceAccountsListUpdate(
        action: AssetAction,
        transactionTarget: TransactionTarget,
        shouldResetBackStack: Boolean = false
    ): Disposable =
        interactor.getAvailableSourceAccounts(action, transactionTarget).subscribeBy(
            onSuccess = {
                process(
                    TransactionIntent.AvailableSourceAccountsListUpdated(it)
                )
                if (shouldResetBackStack) {
                    process(TransactionIntent.ClearBackStack)
                }
            },
            onError = {
                Timber.e("Error getting available accounts for action $action")
            }
        )

    override fun onScanLoopError(t: Throwable) {
        super.onScanLoopError(TxFlowLogError.LoopFail(t))
        Timber.e(t, "!TRANSACTION!> Transaction Model: state error")
        throw t
    }

    override fun onStateUpdate(s: TransactionState) {
        Timber.v("!TRANSACTION!> Transaction Model: state update -> %s", s)
    }

    private fun processInvalidateTransaction(): Disposable =
        interactor.invalidateTransaction()
            .subscribeBy(
                onComplete = {
                    process(TransactionIntent.ReturnToPreviousStep)
                },
                onError = { t ->
                    errorLogger.log(TxFlowLogError.ResetFail(t))
                    process(TransactionIntent.FatalTransactionError(t))
                }
            )

    private fun processInvalidationAndNavigate(state: TransactionState): Disposable =
        interactor.invalidateTransaction()
            .subscribeBy(
                onComplete = {
                    process(
                        TransactionIntent.ReInitialiseWithTargetAndNoSource(
                            state.action, state.selectedTarget, state.passwordRequired
                        )
                    )
                },
                onError = { t ->
                    errorLogger.log(TxFlowLogError.ResetFail(t))
                    process(TransactionIntent.FatalTransactionError(t))
                }
            )

    private fun processPasswordValidation(password: String) =
        interactor.validatePassword(password)
            .subscribeBy(
                onSuccess = {
                    process(
                        if (it) {
                            TransactionIntent.UpdatePasswordIsValidated(password)
                        } else {
                            TransactionIntent.UpdatePasswordNotValidated
                        }
                    )
                },
                onError = { /* Error! What to do? Abort? Or... */ }
            )

    private fun processValidateAddress(
        address: String,
        expectedAsset: AssetInfo
    ): Disposable =
        interactor.validateTargetAddress(address, expectedAsset)
            .subscribeBy(
                onSuccess = {
                    process(TransactionIntent.TargetAddressValidated(it))
                },
                onError = { t ->
                    errorLogger.log(TxFlowLogError.AddressFail(t))
                    when (t) {
                        is TxValidationFailure -> process(TransactionIntent.TargetAddressOrDomainInvalid(t))
                        else -> process(TransactionIntent.FatalTransactionError(t))
                    }
                }
            )

    private fun fetchProductEligibility(
        action: AssetAction,
        sourceAccount: BlockchainAccount,
        target: TransactionTarget
    ): Maybe<FeatureAccess> = when (action) {
        AssetAction.Buy -> interactor.userAccessForFeature(Feature.Buy).toMaybe()
        AssetAction.Swap ->
            interactor.userAccessForFeature(Feature.Swap)
                .flatMap { access ->
                    if (access is FeatureAccess.Granted &&
                        sourceAccount is NonCustodialAccount &&
                        target is TradingAccount
                    ) interactor.userAccessForFeature(Feature.CryptoDeposit)
                    else Single.just(access)
                }.toMaybe()
        AssetAction.Send ->
            if (sourceAccount is NonCustodialAccount && (target is TradingAccount || target is InterestAccount)) {
                interactor.userAccessForFeature(Feature.CryptoDeposit).toMaybe()
            } else {
                Maybe.empty()
            }
        AssetAction.Receive,
        AssetAction.ViewActivity,
        AssetAction.ViewStatement,
        AssetAction.Sell,
        AssetAction.Withdraw,
        AssetAction.InterestDeposit,
        AssetAction.InterestWithdraw,
        AssetAction.FiatDeposit,
        AssetAction.Sign -> Maybe.empty()
    }

    // At this point we can build a transactor object from coincore and configure
    // the state object a bit more; depending on whether it's an internal, external,
    // bitpay or BTC Url address we can set things like note, amount, fee schedule
    // and hook up the correct processor to execute the transaction.
    private fun processTargetSelectionConfirmed(
        sourceAccount: BlockchainAccount,
        amount: Money,
        transactionTarget: TransactionTarget,
        action: AssetAction
    ): Disposable =
        entitySwitchSilverEligibilityFeatureFlag.enabled
            .flatMapMaybe { enabled ->
                if (enabled) fetchProductEligibility(action, sourceAccount, transactionTarget)
                else Maybe.empty()
            }
            .subscribeBy(
                onSuccess = {
                    if (it is FeatureAccess.Blocked) {
                        process(TransactionIntent.ShowKycUpgradeNow)
                    } else {
                        process(
                            TransactionIntent.InitialiseTransaction(
                                sourceAccount,
                                amount,
                                transactionTarget,
                                action,
                                it
                            )
                        )
                    }
                },
                onComplete = {
                    process(TransactionIntent.InitialiseTransaction(sourceAccount, amount, transactionTarget, action))
                },
                onError = {
                    process(TransactionIntent.InitialiseTransaction(sourceAccount, amount, transactionTarget, action))
                    Timber.i(it)
                }
            )

    private fun initialiseTransaction(
        sourceAccount: BlockchainAccount,
        amount: Money,
        transactionTarget: TransactionTarget,
        action: AssetAction,
        eligibility: FeatureAccess?
    ): Disposable =
        interactor.initialiseTransaction(sourceAccount, transactionTarget, action)
            .doOnFirst {
                if (it.validationState == ValidationState.UNINITIALISED ||
                    it.validationState == ValidationState.CAN_EXECUTE
                ) {
                    onFirstUpdate(amount)
                }
            }
            .subscribeBy(
                onNext = {
                    val transactionsLimit = (eligibility as? FeatureAccess.Granted)?.transactionsLimit
                    process(TransactionIntent.PendingTxUpdated(it.copy(transactionsLimit = transactionsLimit)))
                },
                onError = {
                    Timber.e("!TRANSACTION!> Processor failed: $it")
                    errorLogger.log(TxFlowLogError.TargetFail(it))
                    process(TransactionIntent.FatalTransactionError(it))
                }
            )

    private fun onFirstUpdate(amount: Money) {
        process(TransactionIntent.PendingTransactionStarted(interactor.canTransactFiat))
        process(TransactionIntent.FetchFiatRates)
        process(TransactionIntent.FetchTargetRates)
        process(TransactionIntent.AmountChanged(amount))
    }

    private fun processAmountChanged(amount: Money): Disposable =
        interactor.updateTransactionAmount(amount)
            .subscribeBy(
                onError = {
                    Timber.e("!TRANSACTION!> Unable to get update available balance")
                    errorLogger.log(TxFlowLogError.BalanceFail(it))
                    process(TransactionIntent.FatalTransactionError(it))
                }
            )

    private fun processSetFeeLevel(intent: TransactionIntent.SetFeeLevel): Disposable =
        interactor.updateTransactionFees(intent.feeLevel, intent.customFeeAmount)
            .subscribeBy(
                onError = {
                    Timber.e("!TRANSACTION!> Unable to set TX fee level")
                    errorLogger.log(TxFlowLogError.FeesFail(it))
                    process(TransactionIntent.FatalTransactionError(it))
                }
            )

    private fun processExecuteTransaction(secondPassword: String): Disposable =
        interactor.verifyAndExecute(secondPassword)
            .subscribeBy(
                onComplete = {
                    process(TransactionIntent.UpdateTransactionComplete)
                },
                onError = {
                    if (it is NeedsApprovalException) {
                        interactor.updateFiatDepositState(it.bankPaymentData)
                        process(TransactionIntent.ApprovalRequired(it.bankPaymentData))
                    } else {
                        Timber.d("!TRANSACTION!> Unable to execute transaction: $it")
                        errorLogger.log(TxFlowLogError.ExecuteFail(it))
                        process(TransactionIntent.FatalTransactionError(it))
                    }
                }
            )

    private fun processCancelTransaction(): Disposable =
        interactor.cancelTransaction().subscribeBy(
            onComplete = {
                process(TransactionIntent.UpdateTransactionCancelled)
            }, onError = {
            Timber.d("!TRANSACTION!> Unable to cancel transaction: $it")
            errorLogger.log(TxFlowLogError.ExecuteFail(it))
        }
        )

    private fun processModifyTxOptionRequest(newConfirmation: TxConfirmationValue): Disposable =
        interactor.modifyOptionValue(
            newConfirmation
        ).subscribeBy(
            onError = {
                Timber.e("Failed updating Tx options")
            }
        )

    private fun processGetFiatRate(): Disposable =
        interactor.startFiatRateFetch()
            .subscribeBy(
                onNext = { process(TransactionIntent.FiatRateUpdated(it)) },
                onComplete = { Timber.d("Fiat exchange Rate completed") },
                onError = { Timber.e("Failed getting fiat exchange rate") }
            )

    private fun processGetTargetRate(): Disposable =
        interactor.startTargetRateFetch()
            .subscribeBy(
                onNext = { process(TransactionIntent.CryptoRateUpdated(it)) },
                onComplete = { Timber.d("Target exchange Rate completed") },
                onError = { Timber.e("Failed getting target exchange rate") }
            )

    private fun processValidateTransaction(): Disposable? =
        Completable.defer {
            interactor.validateTransaction()
        }.subscribeBy(
            onError = {
                Timber.e("!TRANSACTION!> Unable to validate transaction: $it")
                errorLogger.log(TxFlowLogError.ValidateFail(it))
                process(TransactionIntent.FatalTransactionError(it))
            },
            onComplete = {
                Timber.d("!TRANSACTION!> Tx validation complete")
            }
        )

    private fun processFiatDepositOptions(previousState: TransactionState) =
        interactor.updateFiatDepositOptions((previousState.selectedTarget as FiatCustodialAccount).currency)
            .subscribeBy(
                onSuccess = {
                    process(it)
                },
                onError = {
                    process(TransactionIntent.FiatDepositOptionSelected(DepositOptionsState.Error(it)))
                    Timber.e("Error getting Fiat deposit options. $it")
                }
            )

    private fun processLoadSendToDomainPrefs(prefsKey: String) =
        interactor.loadSendToDomainAnnouncementPref(prefsKey)
            .subscribeBy(
                onSuccess = {
                    process(TransactionIntent.SendToDomainPrefLoaded(it))
                },
                onError = {
                    Timber.e(it)
                }
            )

    private fun processDismissSendToDomainPrefs(prefsKey: String) =
        interactor.dismissSendToDomainAnnouncementPref(prefsKey)
            .subscribeBy(
                onSuccess = {
                    process(TransactionIntent.SendToDomainPrefLoaded(it))
                },
                onError = {
                    Timber.e(it)
                }
            )

    override fun distinctIntentFilter(
        previousIntent: TransactionIntent,
        nextIntent: TransactionIntent
    ): Boolean {
        return when (previousIntent) {
            // Allow consecutive ReturnToPreviousStep intents
            is TransactionIntent.ReturnToPreviousStep -> {
                if (nextIntent is TransactionIntent.ReturnToPreviousStep) {
                    false
                } else {
                    super.distinctIntentFilter(previousIntent, nextIntent)
                }
            }
            else -> super.distinctIntentFilter(previousIntent, nextIntent)
        }
    }
}

private var firstCall = true
fun <T> Observable<T>.doOnFirst(onAction: (T) -> Unit): Observable<T> {
    firstCall = true
    return this.doOnNext {
        if (firstCall) {
            onAction.invoke(it)
            firstCall = false
        }
    }
}
