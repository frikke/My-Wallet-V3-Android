package piuk.blockchain.android.ui.transactionflow.engine

import com.blockchain.api.NabuApiException
import com.blockchain.api.NabuApiExceptionFactory
import com.blockchain.api.isInternetConnectionError
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.EarnRewardsAccount
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.InvoiceTarget
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
import com.blockchain.coincore.eth.WalletConnectTarget
import com.blockchain.commonarch.presentation.mvi.MviIntent
import com.blockchain.domain.paymentmethods.model.BankPaymentApproval
import com.blockchain.domain.paymentmethods.model.DepositTerms
import com.blockchain.domain.paymentmethods.model.FundsLocks
import com.blockchain.domain.paymentmethods.model.LinkBankTransfer
import com.blockchain.domain.trade.model.QuickFillRoundingData
import com.blockchain.nabu.BlockedReason
import com.blockchain.nabu.FeatureAccess
import com.blockchain.nabu.datamanagers.TransactionError
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CurrencyType
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import java.util.Stack
import retrofit2.HttpException

sealed class TransactionIntent : MviIntent<TransactionState> {

    // The InitialiseXYZ intents are data classes so the TransactionFlowIntentMapperTest can compare them
    data class InitialiseWithSourceAccount(
        val action: AssetAction,
        val fromAccount: SingleAccount,
        private val passwordRequired: Boolean
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            TransactionState(
                action = action,
                sendingAccount = fromAccount,
                errorState = TransactionErrorState.NONE,
                passwordRequired = passwordRequired,
                nextEnabled = passwordRequired
            )
    }

    data class InitialiseWithNoSourceOrTargetAccount(
        val action: AssetAction,
        private val passwordRequired: Boolean
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            TransactionState(
                action = action,
                passwordRequired = passwordRequired,
                errorState = TransactionErrorState.NONE
            )
    }

    data class InitialiseWithSourceAndTargetAccount(
        val action: AssetAction,
        val fromAccount: SingleAccount,
        val target: TransactionTarget,
        val passwordRequired: Boolean
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            TransactionState(
                action = action,
                sendingAccount = fromAccount,
                selectedTarget = target,
                errorState = TransactionErrorState.NONE,
                passwordRequired = passwordRequired,
                nextEnabled = passwordRequired
            ).updateBackstack(oldState)
    }

    data class InitialiseWithSourceAndPreferredTarget(
        val action: AssetAction,
        val fromAccount: SingleAccount,
        val target: TransactionTarget,
        private val passwordRequired: Boolean
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            TransactionState(
                action = action,
                sendingAccount = fromAccount,
                selectedTarget = target,
                errorState = TransactionErrorState.NONE,
                passwordRequired = passwordRequired,
                currentStep = TransactionStep.ENTER_ADDRESS,
                nextEnabled = true
            ).updateBackstack(oldState)
    }

    data class InitialiseWithTargetAndNoSource(
        val action: AssetAction,
        val target: TransactionTarget,
        private val passwordRequired: Boolean
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            TransactionState(
                action = action,
                selectedTarget = target,
                errorState = TransactionErrorState.NONE,
                passwordRequired = passwordRequired,
                nextEnabled = true
            )
    }

    data class ReInitialiseWithTargetAndNoSource(
        val action: AssetAction,
        val target: TransactionTarget,
        private val passwordRequired: Boolean
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                action = action,
                selectedTarget = target,
                errorState = TransactionErrorState.NONE,
                passwordRequired = passwordRequired,
                nextEnabled = false,
                stepsBackStack = Stack()
            )
    }

    data class InitialiseTransaction(
        val sourceAccount: BlockchainAccount,
        val amount: Money,
        val transactionTarget: TransactionTarget,
        val action: AssetAction,
        private val passwordRequired: Boolean,
        val eligibility: FeatureAccess? = null,
        private val quickFillRoundingData: List<QuickFillRoundingData> = emptyList()
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState = oldState.copy(
            currentStep = selectStep(passwordRequired, transactionTarget),
            quickFillRoundingData = quickFillRoundingData
        ).updateBackstack(oldState)

        private fun selectStep(
            passwordRequired: Boolean,
            target: TransactionTarget
        ): TransactionStep =
            when {
                passwordRequired -> TransactionStep.ENTER_PASSWORD
                target is InvoiceTarget -> TransactionStep.CONFIRM_DETAIL
                target is WalletConnectTarget -> TransactionStep.CONFIRM_DETAIL
                sourceAccount is EarnRewardsAccount.Active -> TransactionStep.CONFIRM_DETAIL
                else -> TransactionStep.ENTER_AMOUNT
            }
    }

    object ClearBackStack : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState = oldState.copy(
            stepsBackStack = Stack()
        )
    }

    object ResetFlow : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState = oldState.copy(
            currentStep = TransactionStep.CLOSED
        )
    }

    class ValidatePassword(
        val password: String
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                nextEnabled = false,
                errorState = TransactionErrorState.NONE
            ).updateBackstack(oldState)
    }

    class UpdatePasswordIsValidated(
        val password: String
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                nextEnabled = true,
                secondPassword = password,
                currentStep = selectStep(oldState)
            ).updateBackstack(oldState)

        private fun selectStep(oldState: TransactionState): TransactionStep =
            when (oldState.selectedTarget) {
                is NullAddress -> TransactionStep.ENTER_ADDRESS
                is InvoiceTarget -> TransactionStep.CONFIRM_DETAIL
                else -> TransactionStep.ENTER_AMOUNT
            }
    }

    object UpdatePasswordNotValidated : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                nextEnabled = false,
                errorState = TransactionErrorState.INVALID_PASSWORD,
                secondPassword = ""
            ).updateBackstack(oldState)
    }

    class AvailableAccountsListUpdated(private val targets: List<TransactionTarget>) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                availableTargets = targets,
                selectedTarget = NullAddress,
                currentStep = selectStep(oldState.passwordRequired),
                isLoading = false
            ).updateBackstack(oldState)

        private fun selectStep(passwordRequired: Boolean): TransactionStep =
            when {
                passwordRequired -> TransactionStep.ENTER_PASSWORD
                else -> TransactionStep.ENTER_ADDRESS
            }
    }

    class AvailableSourceAccountsListUpdated(private val accounts: List<BlockchainAccount>) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                availableSources = accounts,
                currentStep = TransactionStep.SELECT_SOURCE,
                isLoading = false
            ).updateBackstack(oldState)
    }

    // Check a manually entered address is correct. If it is, the interactor will send a
    // TargetAddressValidated intent which, in turn, will enable the next cta on the enter
    // address sheet
    class ValidateInputTargetAddress(
        val targetAddress: String,
        val expectedCrypto: AssetInfo
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState = oldState
    }

    class TargetAddressValidated(
        private val transactionTarget: TransactionTarget
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                errorState = TransactionErrorState.NONE,
                selectedTarget = transactionTarget,
                nextEnabled = true
            ).updateBackstack(oldState)
    }

    class TargetAddressOrDomainInvalid(private val error: TxValidationFailure) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                errorState = error.state.mapToTransactionError(),
                selectedTarget = NullCryptoAccount(),
                nextEnabled = false
            ).updateBackstack(oldState)
    }

    // Fired from the enter address sheet when a target address is confirmed - by selecting from the list
    // (in this build, this will change for swap) or when the CTA is clicked. Move to the enter amount sheet
    // once this has been processed. Do not send this from anywhere _but_ the enter address sheet.
    object TargetSelected : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                errorState = TransactionErrorState.NONE,
                currentStep = TransactionStep.ENTER_AMOUNT,
                nextEnabled = false
            ).updateBackstack(oldState)
    }

    class TargetSelectionUpdated(
        private val transactionTarget: TransactionTarget
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                errorState = TransactionErrorState.NONE,
                selectedTarget = transactionTarget,
                nextEnabled = true
            ).updateBackstack(oldState)
    }

    object ShowTargetSelection : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                errorState = TransactionErrorState.NONE,
                currentStep = TransactionStep.SELECT_TARGET_ACCOUNT,
                nextEnabled = false
            ).updateBackstack(oldState)
    }

    object ShowSourceSelection : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                errorState = TransactionErrorState.NONE,
                currentStep = TransactionStep.SELECT_SOURCE,
                nextEnabled = false
            ).updateBackstack(oldState)
    }

    object FetchFiatRates : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState = oldState
    }

    object FetchTargetRates : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState = oldState
    }

    object FetchConfirmationRates : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState = oldState
    }

    class FiatRateUpdated(
        private val fiatRate: ExchangeRate
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                fiatRate = fiatRate
            ).updateBackstack(oldState)
    }

    class CryptoRateUpdated(
        private val targetRate: ExchangeRate
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                targetRate = targetRate
            ).updateBackstack(oldState)
    }

    class ConfirmationRateUpdated(
        private val confirmationRate: ExchangeRate?
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                confirmationRate = confirmationRate
            ).updateBackstack(oldState)
    }

    // Send by the interactor when the transaction engine is started, informs the FE that amount input
    // can be performed and provides any capability flags to the FE
    class PendingTransactionStarted(
        private val canTransactFiat: Boolean
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                errorState = TransactionErrorState.NONE,
                allowFiatInput = canTransactFiat,
                nextEnabled = false
            ).updateBackstack(oldState)
    }

    class TargetAccountSelected(
        private val selectedTarget: TransactionTarget
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                currentStep = TransactionStep.ENTER_ADDRESS,
                selectedTarget = selectedTarget,
                nextEnabled = true
            ).updateBackstack(oldState)
    }

    class SourceAccountSelected(
        val sourceAccount: SingleAccount
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                sendingAccount = sourceAccount
            )
    }

    class AmountChanged(
        val amount: Money
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                setMax = false
            ).updateBackstack(oldState)
    }

    class UpdatePrefillAmount(
        private val amounts: PrefillAmounts
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(amountsToPrefill = amounts)
    }

    object ResetPrefillAmount : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState = oldState.copy(amountsToPrefill = null)
    }

    object UseMaxSpendable : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(setMax = true)
    }

    object ResetUseMaxSpendable : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(setMax = false)
    }

    class ModifyTxOption(
        val confirmation: TxConfirmationValue
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState = oldState
    }

    class PendingTxUpdated(
        private val pendingTx: PendingTx
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                pendingTx = pendingTx,
                nextEnabled = pendingTx.validationState == ValidationState.CAN_EXECUTE,
                errorState = pendingTx.validationState.mapToTransactionError()
            ).updateBackstack(oldState)
    }

    class DisplayModeChanged(
        private val inputCurrency: CurrencyType
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                currencyType = inputCurrency
            )
    }

    // Fired when the cta of the enter amount sheet is clicked. This just moved to the
    // confirm sheet, with CTA disabled pending a validation check.
    object PrepareTransaction : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                nextEnabled = false, // Don't enable until we get a validated pendingTx from the interactor
                currentStep = TransactionStep.CONFIRM_DETAIL
            ).updateBackstack(oldState)
    }

    object ExecuteTransaction : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                nextEnabled = false,
                currentStep = TransactionStep.IN_PROGRESS,
                executionStatus = TxExecutionStatus.InProgress
            ).updateBackstack(oldState)
    }

    object CancelTransaction : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                nextEnabled = false
            ).updateBackstack(oldState)
    }

    class FatalTransactionError(
        private val error: Throwable
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState {
            val error = when {
                error is TransactionError -> error
                error is NabuApiException -> TransactionError.HttpError(error)
                error is HttpException -> TransactionError.HttpError(NabuApiExceptionFactory.fromResponseBody(error))
                error.isInternetConnectionError() -> TransactionError.InternetConnectionError
                else -> throw error
            }
            return oldState.copy(
                nextEnabled = true,
                currentStep = TransactionStep.IN_PROGRESS,
                executionStatus = TxExecutionStatus.Error(error)
            ).updateBackstack(oldState)
        }
    }

    class ApprovalRequired(
        private val bankApprovalData: BankPaymentApproval
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                currentStep = TransactionStep.IN_PROGRESS,
                executionStatus = TxExecutionStatus.ApprovalRequired(bankApprovalData)
            ).updateBackstack(oldState)
    }

    data class SetFeeLevel(
        val feeLevel: FeeLevel,
        val customFeeAmount: Long?
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState
    }

    object StartLinkABank : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState = oldState
    }

    object CheckAvailableOptionsForFiatDeposit : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState = oldState.copy(
            depositOptionsState = DepositOptionsState.None
        )
    }

    data class FiatDepositOptionSelected(
        private val depositOptionsState: DepositOptionsState
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                depositOptionsState = depositOptionsState
            )
    }

    object RefreshSourceAccounts : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState = oldState.copy(
            linkBankState = BankLinkingState.NotStarted
        )
    }

    class LinkBankInfoSuccess(private val bankTransferInfo: LinkBankTransfer) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                linkBankState = BankLinkingState.Success(bankTransferInfo)
            )
    }

    class LinkBankFailed(private val e: Throwable) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                linkBankState = BankLinkingState.Error(e)
            )
    }

    object InvalidateTransaction : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                pendingTx = null,
                selectedTarget = NullAddress,
                nextEnabled = false,
                fiatRate = null,
                targetRate = null
            ).updateBackstack(oldState)
    }

    object NavigateBackFromEnterAmount : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState = oldState
    }

    object InvalidateTransactionKeepingTarget : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                pendingTx = null,
                sendingAccount = NullCryptoAccount(),
                nextEnabled = false,
                fiatRate = null,
                targetRate = null
            ).updateBackstack(oldState)
    }

    object UpdateTransactionComplete : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                nextEnabled = true,
                executionStatus = TxExecutionStatus.Completed
            ).updateBackstack(oldState)
    }

    object UpdateTransactionCancelled : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                executionStatus = TxExecutionStatus.Cancelled
            ).updateBackstack(oldState)
    }

    object TransactionApprovalDenied : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                nextEnabled = true,
                executionStatus = TxExecutionStatus.Error(TransactionError.TransactionDenied)
            )
    }

    object ApprovalTriggered : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                executionStatus = TxExecutionStatus.InProgress
            )
    }

    // This fn pops the backstack, thus no need to update the backstack here
    object ReturnToPreviousStep : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState {
            val stack = oldState.stepsBackStack
            require(stack.isNotEmpty())

            val previousStep = stack.pop()
            return oldState.copy(
                stepsBackStack = stack,
                currentStep = previousStep,
                errorState = TransactionErrorState.NONE
            )
        }
    }

    object ShowMoreAccounts : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                currentStep = TransactionStep.SELECT_TARGET_ACCOUNT,
                selectedTarget = NullAddress
            ).updateBackstack(oldState)
    }

    data class ShowFeatureBlocked(val reason: BlockedReason) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                currentStep = TransactionStep.FEATURE_BLOCKED,
                featureBlockedReason = reason
            )
    }

    object ClearSelectedTarget : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                selectedTarget = NullAddress
            )
    }

    // Fired from when the confirm transaction sheet is created.
    // Forces a validation pass; we will get a
    object ValidateTransaction : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState = oldState
    }

    object EnteredAddressReset : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState = oldState.copy(
            errorState = TransactionErrorState.NONE
        )
    }

    fun TransactionState.updateBackstack(oldState: TransactionState) =
        if (oldState.currentStep != this.currentStep && oldState.currentStep.addToBackStack) {
            val updatedStack = oldState.stepsBackStack
            updatedStack.push(oldState.currentStep)

            this.copy(stepsBackStack = updatedStack)
        } else {
            this
        }

    object LoadFundsLocked : TransactionIntent() {

        override fun isValidFor(oldState: TransactionState): Boolean = oldState.locks == null

        override fun reduce(oldState: TransactionState): TransactionState = oldState
    }

    class FundsLocksLoaded(
        private val fundsLocks: FundsLocks
    ) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                locks = fundsLocks
            )
    }

    data class LoadSendToDomainBannerPref(val prefsKey: String) : TransactionIntent() {
        override fun reduce(oldState: TransactionState) = oldState
    }

    data class DismissSendToDomainBanner(val prefsKey: String) : TransactionIntent() {
        override fun reduce(oldState: TransactionState) = oldState
    }

    data class SendToDomainPrefLoaded(private val shouldShowSendToDomain: Boolean) : TransactionIntent() {
        override fun reduce(oldState: TransactionState) = oldState.copy(
            shouldShowSendToDomainBanner = shouldShowSendToDomain
        )
    }

    class SwitchAccountType(val showTrading: Boolean) : TransactionIntent() {
        override fun reduce(oldState: TransactionState) = oldState.copy(
            selectedTarget = NullAddress,
            nextEnabled = false,
            isLoading = true,
            showTradingAccounts = showTrading
        )

        override fun isValidFor(oldState: TransactionState): Boolean {
            return if (showTrading) {
                oldState.availableTargets.any { it is TradingAccount } &&
                    oldState.availableTargets.any { it is NonCustodialAccount } ||
                    oldState.availableTargets.none { it is TradingAccount }
            } else {
                oldState.availableTargets.any { it is TradingAccount } &&
                    oldState.availableTargets.any { it is NonCustodialAccount } ||
                    oldState.availableTargets.none { it is NonCustodialAccount }
            }
        }
    }

    class UpdateTradingAccountsFilterState(private val canFilterTradingAccounts: Boolean) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                canSwitchBetweenAccountType = canFilterTradingAccounts
            )
    }

    class UpdatePrivateKeyAccountsFilterState(private val isPkwAccountFilterActive: Boolean) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                isPkwAccountFilterActive = isPkwAccountFilterActive
            )
    }

    class UpdatePrivateKeyFilter(val isPkwAccountFilterActive: Boolean) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                isPkwAccountFilterActive = isPkwAccountFilterActive
            )

        override fun isValidFor(oldState: TransactionState): Boolean =
            isPkwAccountFilterActive != oldState.isPkwAccountFilterActive
    }

    object LoadDepositTerms : TransactionIntent() {

        override fun isValidFor(oldState: TransactionState): Boolean = oldState.depositTerms == null

        override fun reduce(oldState: TransactionState): TransactionState = oldState
    }

    data class DepositTermsReceived(private val depositTerms: DepositTerms) : TransactionIntent() {

        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                depositTerms = depositTerms
            )
    }

    object LoadImprovedPaymentUxFeatureFlag : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState = oldState
    }

    data class ImprovedPaymentUxFeatureFlagLoaded(private val isLoadImprovedPaymentUxFeatureFlagEnabled: Boolean) :
        TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                ffImprovedPaymentUxEnabled = isLoadImprovedPaymentUxFeatureFlagEnabled
            )
    }

    class UpdateStakingWithdrawalSeen(val networkTicker: String) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState = oldState
    }

    object LoadRewardsWithdrawalUnbondingDays : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState = oldState
    }

    class RewardsWithdrawalUnbondingDaysLoaded(val unbondingDays: Int) : TransactionIntent() {
        override fun reduce(oldState: TransactionState): TransactionState =
            oldState.copy(
                earnWithdrawalUnbondingDays = unbondingDays
            )
    }
}

private fun ValidationState.mapToTransactionError() =
    when (this) {
        ValidationState.INVALID_AMOUNT -> TransactionErrorState.INVALID_AMOUNT
        ValidationState.INSUFFICIENT_FUNDS -> TransactionErrorState.INSUFFICIENT_FUNDS
        ValidationState.INSUFFICIENT_GAS -> TransactionErrorState.NOT_ENOUGH_GAS
        ValidationState.CAN_EXECUTE -> TransactionErrorState.NONE
        ValidationState.UNINITIALISED -> TransactionErrorState.NONE
        ValidationState.INVALID_ADDRESS -> TransactionErrorState.INVALID_ADDRESS
        ValidationState.INVALID_DOMAIN -> TransactionErrorState.INVALID_DOMAIN
        ValidationState.ADDRESS_IS_CONTRACT -> TransactionErrorState.ADDRESS_IS_CONTRACT
        ValidationState.UNDER_MIN_LIMIT -> TransactionErrorState.BELOW_MIN_LIMIT
        ValidationState.PENDING_ORDERS_LIMIT_REACHED -> TransactionErrorState.PENDING_ORDERS_LIMIT_REACHED
        ValidationState.HAS_TX_IN_FLIGHT -> TransactionErrorState.TRANSACTION_IN_FLIGHT
        ValidationState.OPTION_INVALID,
        ValidationState.MEMO_INVALID -> TransactionErrorState.TX_OPTION_INVALID
        ValidationState.OVER_SILVER_TIER_LIMIT -> TransactionErrorState.OVER_SILVER_TIER_LIMIT
        ValidationState.OVER_GOLD_TIER_LIMIT -> TransactionErrorState.OVER_GOLD_TIER_LIMIT
        ValidationState.ABOVE_PAYMENT_METHOD_LIMIT -> TransactionErrorState.ABOVE_MAX_PAYMENT_METHOD_LIMIT
        ValidationState.INVOICE_EXPIRED -> TransactionErrorState.INVALID_ADDRESS
    }
