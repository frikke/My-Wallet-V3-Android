package piuk.blockchain.android.ui.transactionflow.analytics

import com.blockchain.analytics.Analytics
import com.blockchain.analytics.events.LaunchOrigin
import com.blockchain.coincore.AssetAction
import com.blockchain.coincore.BankAccount
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.CryptoAddress
import com.blockchain.coincore.EarnRewardsAccount
import com.blockchain.coincore.FeeLevel
import com.blockchain.coincore.FiatAccount
import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.coincore.NullAddress
import com.blockchain.coincore.NullCryptoAccount
import com.blockchain.coincore.SingleAccount
import com.blockchain.coincore.TradingAccount
import com.blockchain.coincore.TransactionTarget
import com.blockchain.coincore.fiat.LinkedBankAccount
import com.blockchain.coincore.impl.CryptoNonCustodialAccount
import com.blockchain.coincore.impl.txEngine.swap.OUTGOING_FEE
import com.blockchain.coincore.impl.txEngine.swap.RECEIVE_AMOUNT
import com.blockchain.domain.paymentmethods.model.PaymentMethodType
import com.blockchain.earn.EarnAnalytics
import com.blockchain.earn.TxFlowAnalyticsAccountType
import com.blockchain.extensions.filterNotNullValues
import com.blockchain.logging.RemoteLogger
import info.blockchain.balance.Currency
import info.blockchain.balance.Money
import java.math.BigDecimal
import piuk.blockchain.android.simplebuy.AmountType
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.engine.TransactionStep
import piuk.blockchain.android.ui.transactionflow.flow.customisations.InfoActionType
import piuk.blockchain.android.ui.transactionflow.flow.customisations.InfoBottomSheetType
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TransactionFlowBottomSheetInfo

const val WALLET_TYPE_NON_CUSTODIAL = "non_custodial"
const val WALLET_TYPE_CUSTODIAL = "custodial"
const val WALLET_TYPE_INTEREST = "interest"
const val WALLET_TYPE_BANK = "bank"
const val WALLET_TYPE_EXTERNAL = "external"
const val WALLET_TYPE_UNKNOWN = "unknown"

class TxFlowAnalytics(
    private val analytics: Analytics,
    private val remoteLogger: RemoteLogger
) {
    // General
    fun onFlowCanceled(state: TransactionState) {
        when (state.action) {
            AssetAction.Send ->
                if (state.currentStep == TransactionStep.CONFIRM_DETAIL) {
                    analytics.logEvent(SendAnalyticsEvent.CancelTransaction)
                }
            AssetAction.Sell ->
                if (state.currentStep == TransactionStep.CONFIRM_DETAIL) {
                    analytics.logEvent(
                        SellAnalyticsEvent(
                            event = SellAnalytics.CancelTransaction,
                            asset = state.sendingAsset,
                            source = state.sendingAccount.toCategory()
                        )
                    )
                }
            AssetAction.Swap ->
                if (state.currentStep == TransactionStep.CONFIRM_DETAIL) {
                    analytics.logEvent(SwapAnalyticsEvents.CancelTransaction)
                }
            AssetAction.InterestDeposit ->
                if (state.currentStep == TransactionStep.CONFIRM_DETAIL) {
                    analytics.logEvent(InterestDepositAnalyticsEvent.CancelTransaction)
                }
            AssetAction.FiatWithdraw ->
                if (state.currentStep == TransactionStep.CONFIRM_DETAIL) {
                    analytics.logEvent(
                        withdrawEvent(
                            WithdrawAnalytics.WITHDRAW_CHECKOUT_CANCEL,
                            (state.sendingAccount as FiatAccount).currency.networkTicker
                        )
                    )
                }
            else -> {
            }
        }
    }

    fun onQuickMaxClicked(state: TransactionState, maxAmount: Money) {
        when (state.action) {
            AssetAction.Sell -> {
                analytics.logEvent(
                    SellQuickFillButtonClicked(
                        amount = maxAmount.toBigDecimal().toString(),
                        amountType = AmountType.MAX,
                        currency = maxAmount.currencyCode
                    )
                )
            }
            AssetAction.Swap -> {
                analytics.logEvent(
                    SwapAnalyticsEvents.SwapQuickFillButtonClicked(
                        amount = maxAmount.toBigDecimal().toString(),
                        amountType = AmountType.MAX,
                        currency = maxAmount.currencyCode
                    )
                )
            }
            else -> {}
        }
    }

    fun onQuickButtonsClicked(state: TransactionState, buttonTapped: Money, position: Int) {
        when (state.action) {
            AssetAction.Sell -> {
                analytics.logEvent(
                    SellQuickFillButtonClicked(
                        amount = buttonTapped.toBigDecimal().toString(),
                        amountType = AmountType.values()[position],
                        currency = buttonTapped.currencyCode
                    )
                )
            }
            AssetAction.Swap -> {
                analytics.logEvent(
                    SwapAnalyticsEvents.SwapQuickFillButtonClicked(
                        amount = buttonTapped.toBigDecimal().toString(),
                        amountType = AmountType.values()[position],
                        currency = buttonTapped.currencyCode
                    )
                )
            }
            else -> {}
        }
    }

    fun onStepChanged(state: TransactionState) {
        when (state.action) {
            AssetAction.Send -> triggerSendScreenEvent(state.currentStep)
            AssetAction.Sell -> triggerSellScreenEvent(state)
            AssetAction.Swap -> triggerSwapScreenEvent(state.currentStep)
            AssetAction.InterestDeposit -> triggerDepositScreenEvent(state.currentStep)
            AssetAction.InterestWithdraw -> triggerInterestWithdrawScreenEvent(state.currentStep)
            AssetAction.FiatWithdraw -> triggerWithdrawScreenEvent(
                state.currentStep,
                (state.sendingAccount as FiatAccount).currency.networkTicker
            )
            else -> {
            }
        }
    }

    fun onTargetAccountSelected(account: BlockchainAccount, state: TransactionState) {
        when (state.action) {
            AssetAction.Swap -> analytics.logEvent(
                SwapAnalyticsEvents.SwapTargetAccountSelected(
                    (account as CryptoAccount).currency.networkTicker,
                    TxFlowAnalyticsAccountType.fromAccount(account)
                )
            )
            AssetAction.FiatWithdraw -> analytics.logEvent(
                WithdrawAnalytics.WithdrawMethodSelected(
                    (state.sendingAccount as FiatAccount).currency.networkTicker,
                    (account as LinkedBankAccount).type
                )
            )
            AssetAction.Send -> if (account is EarnRewardsAccount.Interest) {
                analytics.logEvent(
                    EarnAnalytics.InterestDepositClicked(
                        currency = state.sendingAsset.networkTicker,
                        origin = LaunchOrigin.SEND
                    )
                )
            }
            else -> {
            }
        }
    }

    private fun triggerWithdrawScreenEvent(step: TransactionStep, currency: String) {
        when (step) {
            TransactionStep.SELECT_SOURCE,
            TransactionStep.SELECT_TARGET_ACCOUNT -> analytics.logEvent(
                withdrawEvent(WithdrawAnalytics.WITHDRAW_SHOWN, currency)
            )
            TransactionStep.CONFIRM_DETAIL -> analytics.logEvent(
                withdrawEvent(WithdrawAnalytics.WITHDRAW_CHECKOUT_SHOWN, currency)
            )
            else -> {
            }
        }
    }

    private fun triggerSwapScreenEvent(step: TransactionStep) {
        when (step) {
            TransactionStep.SELECT_SOURCE -> analytics.logEvent(SwapAnalyticsEvents.FromPickerSeen)
            TransactionStep.SELECT_TARGET_ACCOUNT -> analytics.logEvent(SwapAnalyticsEvents.ToPickerSeen)
            TransactionStep.ENTER_ADDRESS -> analytics.logEvent(SwapAnalyticsEvents.SwapTargetAddressSheet)
            TransactionStep.ENTER_AMOUNT -> analytics.logEvent(SwapAnalyticsEvents.SwapEnterAmount)
            TransactionStep.CONFIRM_DETAIL -> analytics.logEvent(SwapAnalyticsEvents.SwapConfirmSeen)
            else -> {
            }
        }
    }

    private fun triggerSendScreenEvent(step: TransactionStep) {
        when (step) {
            TransactionStep.ENTER_ADDRESS -> analytics.logEvent(SendAnalyticsEvent.EnterAddressDisplayed)
            TransactionStep.ENTER_AMOUNT -> analytics.logEvent(SendAnalyticsEvent.EnterAmountDisplayed)
            TransactionStep.CONFIRM_DETAIL -> analytics.logEvent(SendAnalyticsEvent.ConfirmationsDisplayed)
            else -> {
            }
        }
    }

    private fun triggerDepositScreenEvent(step: TransactionStep) {
        when (step) {
            TransactionStep.ENTER_AMOUNT -> {
                analytics.logEvent(EarnAnalytics.InterestDepositViewed)
                analytics.logEvent(InterestDepositAnalyticsEvent.EnterAmountSeen)
            }
            TransactionStep.CONFIRM_DETAIL -> analytics.logEvent(InterestDepositAnalyticsEvent.ConfirmationsSeen)
            else -> {
            }
        }
    }

    private fun triggerInterestWithdrawScreenEvent(step: TransactionStep) {
        when (step) {
            TransactionStep.ENTER_AMOUNT -> analytics.logEvent(EarnAnalytics.InterestWithdrawalViewed)
            else -> {
            }
        }
    }

    private fun triggerSellScreenEvent(state: TransactionState) {
        when (state.currentStep) {
            TransactionStep.CONFIRM_DETAIL -> analytics.logEvent(
                SellAnalyticsEvent(
                    event = SellAnalytics.ConfirmationsDisplayed,
                    asset = state.sendingAsset,
                    source = state.sendingAccount.toCategory()
                )
            )
            else -> {
            }
        }
    }

    // Enter address sheet
    fun onManualAddressEntered(state: TransactionState) {}

    fun onScanQrClicked(state: TransactionState) {
        when (state.action) {
            AssetAction.Send -> analytics.logEvent(SendAnalyticsEvent.QrCodeScanned)
            else -> {
            }
        }
    }

    fun onAccountSelected(account: SingleAccount, state: TransactionState) {
        when (state.action) {
            AssetAction.Send -> analytics.logEvent(SendAnalyticsEvent.EnterAddressCtaClick)
            AssetAction.Swap -> {
                require(account is CryptoAccount)
                analytics.logEvent(SwapAnalyticsEvents.FromAccountSelected)
            }
            else -> {
            }
        }
    }

    fun onFromSourcesAccountViewed(state: AssetAction?) {
        when (state) {
            AssetAction.Swap -> {
                analytics.logEvent(
                    SwapAnalyticsEvents.SwapFromWalletPageViewedEvent
                )
            }
            else -> {}
        }
    }

    fun onSourceAccountSelected(account: BlockchainAccount, state: TransactionState) {
        when (state.action) {
            AssetAction.Swap -> {
                require(account is CryptoAccount)
                analytics.logEvent(
                    SwapAnalyticsEvents.SwapFromSelected(
                        currency = account.currency.networkTicker,
                        accountType = TxFlowAnalyticsAccountType.fromAccount(account)
                    )
                )
            }
            AssetAction.Send -> {
                require(account is CryptoAccount)
                analytics.logEvent(
                    SendAnalyticsEvent.SendSourceAccountSelected(
                        currency = account.currency.networkTicker,
                        fromAccountType = TxFlowAnalyticsAccountType.fromAccount(account)
                    )
                )
            }
            AssetAction.Sell -> {
                require(account is CryptoAccount)
                analytics.logEvent(
                    SellSourceAccountSelected(
                        sourceAccountType = TxFlowAnalyticsAccountType.fromAccount(state.sendingAccount),
                        inputCurrency = account.currency.networkTicker
                    )
                )
            }
            AssetAction.FiatDeposit -> {
                require(account is LinkedBankAccount)
                analytics.logEvent(
                    DepositAnalytics.DepositMethodSelected(
                        currency = account.currency.networkTicker,
                        paymentMethodType = PaymentMethodType.BANK_TRANSFER
                    )
                )
            }
            else -> {
            }
        }
    }

    fun onEnterAddressCtaClick(state: TransactionState) {
        when (state.action) {
            AssetAction.Swap -> {
                analytics.logEvent(
                    SwapAnalyticsEvents.SwapConfirmPair(
                        asset = state.sendingAsset,
                        target = state.selectedTarget.toCategory()
                    )
                )
                analytics.logEvent(
                    SwapAnalyticsEvents.SwapAccountsSelected(
                        inputCurrency = state.sendingAsset,
                        outputCurrency = (state.selectedTarget as CryptoAccount).currency,
                        sourceAccountType = TxFlowAnalyticsAccountType.fromAccount(state.sendingAccount),
                        targetAccountType = TxFlowAnalyticsAccountType.fromAccount(state.selectedTarget),
                        werePreselected = false
                    )
                )
            }
            else -> {
            }
        }
    }

    // Enter amount sheet
    fun onMaxClicked(state: TransactionState) {
        when (state.action) {
            AssetAction.Send -> {
                analytics.logEvent(SendAnalyticsEvent.SendMaxClicked)
                analytics.logEvent(
                    SendAnalyticsEvent.SendAmountMaxClicked(
                        currency = state.sendingAsset.networkTicker,
                        toAccountType = TxFlowAnalyticsAccountType.fromTransactionTarget(state.selectedTarget),
                        fromAccountType = TxFlowAnalyticsAccountType.fromAccount(state.sendingAccount)
                    )
                )
            }
            AssetAction.Swap -> {
                check(state.selectedTarget is CryptoAccount)
                analytics.logEvent(
                    SwapAnalyticsEvents.SwapMaxAmountClicked(
                        sourceCurrency = state.sendingAsset.networkTicker,
                        sourceAccountType = TxFlowAnalyticsAccountType.fromAccount(state.sendingAccount),
                        targetAccountType = TxFlowAnalyticsAccountType.fromTransactionTarget(state.selectedTarget),
                        targetCurrency = state.selectedTarget.currency.networkTicker
                    )
                )
            }
            AssetAction.FiatWithdraw -> analytics.logEvent(
                WithdrawAnalytics.WithdrawalMaxClicked(
                    currency = (state.sendingAccount as FiatAccount).currency.networkTicker,
                    paymentMethodType = (state.sendingAccount as LinkedBankAccount).type
                )
            )
            AssetAction.Sell -> {
                analytics.logEvent(
                    MaxAmountClicked(
                        sourceAccountType = TxFlowAnalyticsAccountType.fromAccount(state.sendingAccount),
                        inputCurrency = state.sendingAsset.networkTicker,
                        outputCurrency = (state.selectedTarget as? FiatAccount)?.currency?.networkTicker ?: run {
                            remoteLogger.logEvent("Target account not set")
                            return
                        }
                    )
                )
            }
            AssetAction.InterestDeposit -> {
                analytics.logEvent(
                    EarnAnalytics.InterestDepositMaxAmount(
                        currency = state.amount.currencyCode,
                        sourceAccountType = TxFlowAnalyticsAccountType.fromAccount(state.sendingAccount)
                    )
                )
            }
            else -> {
            }
        }
    }

    fun onCryptoToggle(inputType: Currency, state: TransactionState) {
        when (state.action) {
            AssetAction.Sell -> analytics.logEvent(SellFiatCryptoSwitcherClickedEvent(inputType))
            AssetAction.Swap -> analytics.logEvent(SwapAnalyticsEvents.SwapFiatCryptoSwitcherClickedEvent(inputType))
            else -> {}
        }
        if (state.action.shouldLogInputSwitch()) {
            analytics.logEvent(
                AmountSwitched(
                    action = state.action,
                    newInput = inputType
                )
            )
        }
    }

    fun onEnterAmountCtaClick(state: TransactionState) {
        when (state.action) {
            AssetAction.Send ->
                analytics.logEvent(SendAnalyticsEvent.EnterAmountCtaClick)
            AssetAction.Sell -> {
                analytics.logEvent(
                    SellAnalyticsEvent(
                        event = SellAnalytics.EnterAmountCtaClick,
                        asset = state.sendingAsset,
                        source = state.sendingAccount.toCategory()
                    )
                )
                analytics.logEvent(
                    SellAmountScreenNextClicked(
                        sourceAccountType = TxFlowAnalyticsAccountType.fromAccount(state.sendingAccount),
                        amount = state.amount,
                        outputCurrency = (state.selectedTarget as FiatAccount).currency.networkTicker
                    )
                )
            }
            AssetAction.InterestDeposit -> {
                analytics.logEvent(InterestDepositAnalyticsEvent.EnterAmountCtaClick(state.sendingAsset))
                analytics.logEvent(
                    EarnAnalytics.InterestDepositAmountEntered(
                        currency = state.sendingAsset.networkTicker,
                        sourceAccountType = TxFlowAnalyticsAccountType.fromAccount(state.sendingAccount),
                        inputAmount = state.amount
                    )
                )
            }
            AssetAction.Swap -> {
                analytics.logEvent(
                    SwapAnalyticsEvents.EnterAmountCtaClick(
                        source = state.sendingAsset,
                        target = state.selectedTarget.toCategory()
                    )
                )
                analytics.logEvent(
                    SwapAnalyticsEvents.SwapAmountEntered(
                        amount = state.amount,
                        inputAccountType = TxFlowAnalyticsAccountType.fromAccount(state.sendingAccount),
                        outputCurrency = (state.selectedTarget as CryptoAccount).currency.networkTicker,
                        outputAccountType = TxFlowAnalyticsAccountType.fromAccount(state.selectedTarget)
                    )
                )
            }
            AssetAction.FiatWithdraw -> {
                analytics.logEvent(
                    withdrawEvent(
                        WithdrawAnalytics.WITHDRAW_CONFIRM,
                        (state.sendingAccount as FiatAccount).currency.networkTicker
                    )
                )
                val amount = state.pendingTx?.amount ?: throw IllegalArgumentException("Amount is missing")
                val fee = state.pendingTx.feeAmount
                analytics.logEvent(
                    WithdrawAnalytics.WithdrawalAmountEntered(
                        netAmount = amount - fee,
                        grossAmount = amount,
                        paymentMethodType = (state.selectedTarget as LinkedBankAccount).type
                    )
                )
            }
            AssetAction.FiatDeposit -> {
                val paymentMethodType =
                    if (state.sendingAccount is BankAccount) PaymentMethodType.BANK_TRANSFER else return
                analytics.logEvent(
                    DepositAnalytics.DepositAmountEntered(
                        currency = (state.sendingAccount as FiatAccount).currency.networkTicker,
                        paymentMethodType = paymentMethodType,
                        amount = state.amount
                    )
                )
            }
            else -> {
            }
        }
    }

    fun onInfoBottomSheetActionClicked(info: TransactionFlowBottomSheetInfo, state: TransactionState) {
        if (
            info.type == InfoBottomSheetType.TRANSACTIONS_LIMIT &&
            info.action?.actionType == InfoActionType.KYC_UPGRADE
        ) {
            analytics.logEvent(InfoBottomSheetKycUpsellActionClicked(state.action))
        }
    }

    fun onInfoBottomSheetDismissed(info: TransactionFlowBottomSheetInfo, state: TransactionState) {
        if (info.type == InfoBottomSheetType.TRANSACTIONS_LIMIT) {
            analytics.logEvent(InfoBottomSheetDismissed(state.action))
        }
    }

    // Confirm sheet
    fun onConfirmationCtaClick(state: TransactionState) {
        when (state.action) {
            AssetAction.Send -> {
                analytics.logEvent(
                    SendAnalyticsEvent.ConfirmTransaction(
                        asset = state.sendingAsset,
                        source = state.sendingAccount.toCategory(),
                        target = state.selectedTarget.toCategory(),
                        feeLevel = state.pendingTx?.feeSelection?.selectedLevel.toString()
                    )
                )
                analytics.logEvent(
                    SendAnalyticsEvent.SendSubmitted(
                        currency = state.sendingAsset.networkTicker,
                        feeType = state.pendingTx?.feeSelection?.selectedLevel?.toAnalyticsFee()
                            ?: AnalyticsFeeType.BACKEND,
                        fromAccountType = TxFlowAnalyticsAccountType.fromAccount(state.sendingAccount),
                        toAccountType = TxFlowAnalyticsAccountType.fromTransactionTarget(state.selectedTarget)
                    )
                )
            }
            AssetAction.InterestDeposit ->
                analytics.logEvent(
                    InterestDepositAnalyticsEvent.ConfirmationsCtaClick(
                        state.sendingAsset
                    )
                )
            AssetAction.Sell -> {
                analytics.logEvent(
                    SellAnalyticsEvent(
                        event = SellAnalytics.ConfirmTransaction,
                        asset = state.sendingAsset,
                        source = state.sendingAccount.toCategory()
                    )
                )
                analytics.logEvent(
                    MaxAmountClicked(
                        sourceAccountType = TxFlowAnalyticsAccountType.fromAccount(state.sendingAccount),
                        inputCurrency = state.sendingAsset.networkTicker,
                        outputCurrency = (state.selectedTarget as FiatAccount).currency.networkTicker
                    )
                )
                analytics.logEvent(SellCheckoutScreenSubmittedEvent)
            }
            AssetAction.Swap -> {
                analytics.logEvent(
                    SwapAnalyticsEvents.SwapConfirmCta(
                        source = state.sendingAsset,
                        target = state.selectedTarget.toCategory()
                    )
                )
                analytics.logEvent(SwapAnalyticsEvents.SwapCheckoutScreenSubmittedEvent)
            }
            AssetAction.FiatWithdraw ->
                analytics.logEvent(
                    withdrawEvent(
                        WithdrawAnalytics.WITHDRAW_CHECKOUT_CONFIRM,
                        (state.sendingAccount as FiatAccount).currency.networkTicker
                    )
                )
            else -> {
            }
        }
    }

    fun onViewAmountScreen(state: AssetAction?) {
        when (state) {
            AssetAction.Sell -> analytics.logEvent(SellAmountScreenViewedEvent)
            else -> {}
        }
    }

    fun onViewCheckoutScreen(state: AssetAction?) {
        when (state) {
            AssetAction.Sell -> analytics.logEvent(SellCheckoutScreenViewedEvent)
            AssetAction.Swap -> analytics.logEvent(SwapAnalyticsEvents.SwapCheckoutScreenViewedEvent)
            else -> {}
        }
    }

    fun onPriceTooltipClicked(state: AssetAction?) {
        when (state) {
            AssetAction.Sell -> analytics.logEvent(SellPriceTooltipClickedEvent)
            AssetAction.Swap -> analytics.logEvent(SwapAnalyticsEvents.SwapPriceTooltipClickedEvent)
            else -> {}
        }
    }

    fun onFeesTooltipClicked(state: AssetAction?) {
        when (state) {
            AssetAction.Sell -> analytics.logEvent(SellCheckoutNetworkFeesClickedEvent)
            AssetAction.Swap -> analytics.logEvent(SwapAnalyticsEvents.SwapCheckoutNetworkFeesClickedEvent)
            else -> {}
        }
    }

    fun onAmountScreenBackClicked(state: TransactionState) {
        when (state.action) {
            AssetAction.Sell -> analytics.logEvent(SellAmountScreenBackClickedEvent)
            AssetAction.Swap -> analytics.logEvent(SwapAnalyticsEvents.SwapAmountBackScreenClickedEvent)
            else -> {}
        }
    }

    fun onCheckoutScreenBackClicked(state: TransactionState) {
        when (state.action) {
            AssetAction.Sell -> analytics.logEvent(SellCheckoutScreenBackClickedEvent)
            AssetAction.Swap -> analytics.logEvent(SwapAnalyticsEvents.SwapCheckoutScreenBackClickedEvent)
            else -> {}
        }
    }

    // Progress sheet
    fun onTransactionSuccess(state: TransactionState) {
        when (state.action) {
            AssetAction.Send ->
                analytics.logEvent(
                    SendAnalyticsEvent.TransactionSuccess(
                        asset = state.sendingAsset,
                        target = state.selectedTarget.toCategory(),
                        source = state.sendingAccount.toCategory()
                    )
                )
            AssetAction.Sell -> analytics.logEvent(
                SellAnalyticsEvent(
                    event = SellAnalytics.TransactionSuccess,
                    asset = state.sendingAsset,
                    source = state.sendingAccount.toCategory()
                )
            )
            AssetAction.InterestDeposit -> analytics.logEvent(
                InterestDepositAnalyticsEvent.TransactionSuccess(state.sendingAsset)
            )
            AssetAction.Swap -> {
                analytics.logEvent(
                    SwapAnalyticsEvents.TransactionSuccess(
                        asset = state.sendingAsset,
                        target = state.selectedTarget.toCategory(),
                        source = state.sendingAccount.toCategory()
                    )
                )
                require(state.pendingTx != null)
                if (
                    state.sendingAccount is CryptoNonCustodialAccount &&
                    state.selectedTarget is CryptoNonCustodialAccount
                ) {
                    analytics.logEvent(
                        SwapAnalyticsEvents.OnChainSwapRequested(
                            exchangeRate = state.confirmationRate?.price?.toBigDecimal() ?: throw IllegalStateException(
                                "Target rate is missing"
                            ),
                            amount = state.pendingTx.amount,
                            inputNetworkFee = state.pendingTx.feeAmount,
                            outputNetworkFee = state.pendingTx.engineState[OUTGOING_FEE]?.let {
                                it as Money
                            } ?: Money.zero(state.selectedTarget.currency),
                            outputAmount = state.pendingTx.engineState[RECEIVE_AMOUNT]?.let {
                                Money.fromMajor(state.sendingAsset, it as BigDecimal)
                            } ?: Money.zero(state.sendingAsset)
                        )
                    )
                }
            }
            AssetAction.FiatWithdraw ->
                analytics.logEvent(
                    withdrawEvent(
                        WithdrawAnalytics.WITHDRAW_SUCCESS,
                        (state.sendingAccount as FiatAccount).currency.networkTicker
                    )
                )
            else -> {
            }
        }
    }

    fun onTransactionFailure(state: TransactionState, error: String) {
        when (state.action) {
            AssetAction.Send -> analytics.logEvent(
                SendAnalyticsEvent.TransactionFailure(
                    asset = state.sendingAsset,
                    target = state.selectedTarget.takeIf { it != NullAddress }?.toCategory(),
                    source = state.sendingAccount.takeIf { it !is NullCryptoAccount }?.toCategory(),
                    error = error
                )
            )
            AssetAction.Sell -> analytics.logEvent(
                SellAnalyticsEvent(
                    event = SellAnalytics.TransactionFailed,
                    asset = state.sendingAsset,
                    source = state.sendingAccount.toCategory()
                )
            )
            AssetAction.InterestDeposit -> analytics.logEvent(
                InterestDepositAnalyticsEvent.TransactionFailed(state.sendingAsset)
            )
            AssetAction.Swap -> analytics.logEvent(
                SwapAnalyticsEvents.TransactionFailed(
                    asset = state.sendingAsset,
                    target = state.selectedTarget.takeIf { it != NullAddress }?.toCategory(),
                    source = state.sendingAccount.takeIf { it !is NullCryptoAccount }?.toCategory(),
                    error = error
                )
            )
            AssetAction.FiatWithdraw ->
                analytics.logEvent(
                    withdrawEvent(
                        WithdrawAnalytics.WITHDRAW_ERROR,
                        (state.sendingAccount as FiatAccount).currency.networkTicker
                    )
                )
            else -> {
            }
        }
    }

    fun onFeeLevelChanged(oldLevel: FeeLevel, newLevel: FeeLevel) {
        if (oldLevel != newLevel) {
            analytics.logEvent(SendAnalyticsEvent.FeeChanged(oldLevel, newLevel))
        }
    }

    companion object {
        internal const val PARAM_ASSET = "asset"
        internal const val PARAM_SOURCE = "source"
        internal const val PARAM_TARGET = "target"
        internal const val PARAM_ERROR = "error"
        internal const val PARAM_OLD_FEE = "old_fee"
        internal const val PARAM_NEW_FEE = "new_fee"
        internal const val FEE_SCHEDULE = "fee_level"

        internal fun constructMap(
            asset: Currency,
            target: String?,
            error: String? = null,
            source: String? = null
        ): Map<String, String> =
            mapOf(
                PARAM_ASSET to asset.networkTicker,
                PARAM_TARGET to target,
                PARAM_SOURCE to source,
                PARAM_ERROR to error
            ).filterNotNullValues()
    }
}

private fun AssetAction.shouldLogInputSwitch(): Boolean =
    when (this) {
        AssetAction.Send,
        AssetAction.InterestWithdraw,
        AssetAction.InterestDeposit -> true
        else -> false
    }

fun SingleAccount.toCategory() =
    when (this) {
        is EarnRewardsAccount.Interest -> WALLET_TYPE_INTEREST
        is TradingAccount -> WALLET_TYPE_CUSTODIAL
        is NonCustodialAccount -> WALLET_TYPE_NON_CUSTODIAL
        is BankAccount -> WALLET_TYPE_BANK
        else -> WALLET_TYPE_UNKNOWN
    }

fun TransactionTarget.toCategory(): String =
    when (this) {
        is CryptoAddress -> WALLET_TYPE_EXTERNAL
        is EarnRewardsAccount.Interest -> WALLET_TYPE_INTEREST
        is TradingAccount -> WALLET_TYPE_CUSTODIAL
        is NonCustodialAccount -> WALLET_TYPE_NON_CUSTODIAL
        is BankAccount -> WALLET_TYPE_BANK
        else -> WALLET_TYPE_UNKNOWN
    }

private fun FeeLevel.toAnalyticsFee(): AnalyticsFeeType =
    when (this) {
        FeeLevel.Custom -> AnalyticsFeeType.CUSTOM
        FeeLevel.Regular -> AnalyticsFeeType.NORMAL
        FeeLevel.None -> AnalyticsFeeType.BACKEND
        FeeLevel.Priority -> AnalyticsFeeType.PRIORITY
    }

enum class AnalyticsFeeType {
    CUSTOM, NORMAL, PRIORITY, BACKEND
}
