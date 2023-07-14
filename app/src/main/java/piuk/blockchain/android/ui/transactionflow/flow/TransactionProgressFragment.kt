package piuk.blockchain.android.ui.transactionflow.flow

import android.app.Activity.RESULT_CANCELED
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.blockchain.api.NabuApiException
import com.blockchain.coincore.AssetAction
import com.blockchain.domain.paymentmethods.model.BankAuthSource
import com.blockchain.domain.paymentmethods.model.SettlementReason
import com.blockchain.nabu.datamanagers.TransactionError
import com.blockchain.presentation.spinner.SpinnerAnalyticsAction
import com.blockchain.presentation.spinner.SpinnerAnalyticsScreen
import com.blockchain.presentation.spinner.SpinnerAnalyticsTimer
import java.util.Locale
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentTxFlowInProgressBinding
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.BELOW_MINIMUM_LIMIT
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.EXECUTION_FAILED
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.INELIGIBLE
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.INSUFFICIENT_FUNDS
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.INTERNAL_SERVER_ERROR
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.INTERNET_CONNECTION_ERROR
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.INVALID_ADDRESS
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.INVALID_AMOUNT
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.INVALID_CRYPTO_CURRENCY
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.INVALID_FIAT_CURRENCY
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.INVALID_POSTCODE
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.INVALID_QUOTE
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.NABU_ERROR
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.ORDER_DIRECTION_DISABLED
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.ORDER_NOT_CANCELABLE
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.OVER_MAXIMUM_PERSONAL_LIMIT
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.OVER_MAXIMUM_SOURCE_LIMIT
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.PENDING_ORDERS_LIMIT_REACHED
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.TRADING_DISABLED
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.UNKNOWN_ERROR
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.WITHDRAW_ALREADY_PENDING
import piuk.blockchain.android.simplebuy.ClientErrorAnalytics.Companion.WITHDRAW_BALANCE_LOCKED
import piuk.blockchain.android.ui.customviews.TransactionProgressView
import piuk.blockchain.android.ui.linkbank.BankAuthActivity
import piuk.blockchain.android.ui.linkbank.BankAuthRefreshContract
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.engine.TxExecutionStatus
import piuk.blockchain.android.ui.transactionflow.flow.customisations.ErrorStateIcon
import piuk.blockchain.android.ui.transactionflow.flow.customisations.SettlementErrorStateAction
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TransactionProgressCustomisations
import timber.log.Timber

class TransactionProgressFragment : TransactionFlowFragment<FragmentTxFlowInProgressBinding>() {

    private val spinnerTimer: SpinnerAnalyticsTimer by inject {
        parametersOf(
            SpinnerAnalyticsScreen.BuyOrder,
            lifecycleScope
        )
    }

    private val refreshBankResultLauncher = registerForActivityResult(BankAuthRefreshContract()) { refreshSuccess ->
        if (refreshSuccess) {
            activity.finish()
        }
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTxFlowInProgressBinding =
        FragmentTxFlowInProgressBinding.inflate(inflater, container, false)

    private val customiser: TransactionProgressCustomisations by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycle.addObserver(spinnerTimer)

        binding.txProgressView.setupPrimaryCta(
            text = getString(com.blockchain.stringResources.R.string.common_ok),
            onClick = { activity.finish() }
        )
    }

    override fun render(newState: TransactionState) {
        Timber.d("!TRANSACTION!> Rendering! TransactionProgressFragment")

        customiser.transactionProgressStandardIcon(newState)?.let {
            binding.txProgressView.setAssetIcon(it)
        } ?: binding.txProgressView.setAssetIcon(newState.sendingAsset)

        handleStatusUpdates(newState)
    }

    private fun handleStatusUpdates(
        newState: TransactionState
    ) {
        when (newState.executionStatus) {
            is TxExecutionStatus.InProgress -> {
                if(newState.action == AssetAction.Buy) {
                    spinnerTimer.start(SpinnerAnalyticsAction.Default)
                }

                binding.txProgressView.showTxInProgress(
                    customiser.transactionProgressTitle(newState),
                    customiser.transactionProgressMessage(newState)
                )
            }
            is TxExecutionStatus.Completed -> {
                if(newState.action == AssetAction.Buy) {
                    spinnerTimer.stop(isDestroyed = false)
                }

                analyticsHooks.onTransactionSuccess(newState)
                binding.txProgressView.showTxSuccess(
                    customiser.transactionCompleteTitle(newState),
                    customiser.transactionCompleteMessage(newState),
                    customiser.transactionCompleteIcon(newState)
                )
            }
            is TxExecutionStatus.ApprovalRequired -> {
                if(newState.action == AssetAction.Buy) {
                    spinnerTimer.start(SpinnerAnalyticsAction.Default)
                }

                binding.txProgressView.showTxInProgress(
                    customiser.transactionProgressTitle(newState),
                    customiser.transactionProgressMessage(newState)
                )
                model.process(TransactionIntent.ApprovalTriggered)
                startActivityForResult(
                    BankAuthActivity.newInstance(
                        newState.executionStatus.approvalData,
                        BankAuthSource.DEPOSIT,
                        requireContext()
                    ),
                    PAYMENT_APPROVAL
                )
            }
            is TxExecutionStatus.Error -> {
                if(newState.action == AssetAction.Buy) {
                    spinnerTimer.stop(isDestroyed = false)
                }
                analyticsHooks.onTransactionFailure(
                    newState,
                    collectStackTraceString(newState.executionStatus.exception)
                )
                customiser.transactionProgressExceptionIcon(newState).run {
                    with(binding.txProgressView) {
                        when (val iconResource = this@run) {
                            is ErrorStateIcon.Local -> {
                                showTxError(
                                    customiser.transactionProgressExceptionTitle(newState),
                                    customiser.transactionProgressExceptionDescription(newState),
                                    iconResource.resourceId
                                )
                            }
                            is ErrorStateIcon.RemoteIcon ->
                                showServerSideError(
                                    iconUrl = iconResource.iconUrl,
                                    title = customiser.transactionProgressExceptionTitle(newState),
                                    description = customiser.transactionProgressExceptionDescription(newState)
                                )
                            is ErrorStateIcon.RemoteIconWithStatus ->
                                showServerSideError(
                                    iconUrl = iconResource.iconUrl,
                                    statusIconUrl = iconResource.statusIconUrl,
                                    title = customiser.transactionProgressExceptionTitle(newState),
                                    description = customiser.transactionProgressExceptionDescription(newState)
                                )
                        }
                        val settlementActions = customiser.transactionSettlementExceptionAction(newState)
                        val actions = customiser.transactionProgressExceptionActions(newState)
                        when {
                            settlementActions != SettlementErrorStateAction.None ->
                                handleActionForErrorState(settlementActions)
                            else -> {
                                showServerSideActionErrorCtas(
                                    list = actions,
                                    currencyCode = newState.sendingAccount.currency.networkTicker,
                                    onActionsClickedCallback = object :
                                        TransactionProgressView.TransactionProgressActions {
                                        override fun onPrimaryButtonClicked() {
                                            activity.finish()
                                        }

                                        override fun onSecondaryButtonClicked() {
                                            activity.finish()
                                        }

                                        override fun onTertiaryButtonClicked() {
                                            activity.finish()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                logClientErrorToAnalytics(newState)
            }
            else -> {
                // do nothing
            }
        }
    }

    override fun onPause() {
        binding.txProgressView.clearSubscriptions()
        super.onPause()
    }

    private fun sendAnalyticsEvent(
        analyticsPair: Pair<String, String>,
        action: String,
        nabuApiException: NabuApiException?,
        errorDescription: String?
    ) {
        analytics.logEvent(
            ClientErrorAnalytics.ClientLogError(
                nabuApiException = nabuApiException,
                errorDescription = errorDescription,
                error = analyticsPair.second,
                source = nabuApiException?.let { ClientErrorAnalytics.Companion.Source.NABU }
                    ?: ClientErrorAnalytics.Companion.Source.CLIENT,
                title = analyticsPair.first,
                action = action,
                categories = nabuApiException?.getServerSideErrorInfo()?.categories ?: emptyList()
            )
        )
    }

    private fun logClientErrorToAnalytics(state: TransactionState) {
        require(state.executionStatus is TxExecutionStatus.Error)
        require(state.executionStatus.exception is TransactionError)
        val error = state.executionStatus.exception

        val nabuApiException = if (error is TransactionError.HttpError) {
            error.nabuApiException
        } else {
            null
        }

        val pair = when (error) {
            TransactionError.OrderLimitReached -> Pair(
                getString(
                    com.blockchain.stringResources.R.string.trading_order_limit,
                    getActionStringResource(state.action)
                ),
                PENDING_ORDERS_LIMIT_REACHED
            )
            TransactionError.OrderNotCancelable -> Pair(
                getString(
                    com.blockchain.stringResources.R.string.trading_order_not_cancelable,
                    getActionStringResource(state.action)
                ),
                ORDER_NOT_CANCELABLE
            )
            TransactionError.WithdrawalAlreadyPending -> Pair(
                getString(com.blockchain.stringResources.R.string.trading_pending_withdrawal),
                WITHDRAW_ALREADY_PENDING
            )
            TransactionError.WithdrawalBalanceLocked -> Pair(
                getString(com.blockchain.stringResources.R.string.trading_withdrawal_balance_locked),
                WITHDRAW_BALANCE_LOCKED
            )
            TransactionError.WithdrawalInsufficientFunds -> Pair(
                getString(com.blockchain.stringResources.R.string.trading_withdrawal_insufficient_funds),
                INSUFFICIENT_FUNDS
            )
            TransactionError.InternalServerError -> {
                Pair(
                    getString(com.blockchain.stringResources.R.string.trading_internal_server_error),
                    INTERNAL_SERVER_ERROR
                )
            }
            TransactionError.TradingTemporarilyDisabled -> Pair(
                getString(com.blockchain.stringResources.R.string.trading_service_temp_disabled),
                TRADING_DISABLED
            )
            TransactionError.InsufficientBalance -> Pair(
                getString(
                    com.blockchain.stringResources.R.string.trading_insufficient_balance,
                    getActionStringResource(state.action)
                ),
                OVER_MAXIMUM_PERSONAL_LIMIT
            )
            TransactionError.OrderBelowMin -> Pair(
                getString(
                    com.blockchain.stringResources.R.string.trading_amount_below_min,
                    getActionStringResource(state.action)
                ),
                BELOW_MINIMUM_LIMIT
            )
            TransactionError.OrderAboveMax -> Pair(
                getString(
                    com.blockchain.stringResources.R.string.trading_amount_above_max,
                    getActionStringResource(state.action)
                ),
                OVER_MAXIMUM_SOURCE_LIMIT
            )
            TransactionError.SwapDailyLimitExceeded -> Pair(
                getString(
                    com.blockchain.stringResources.R.string.trading_daily_limit_exceeded,
                    getActionStringResource(state.action)
                ),
                OVER_MAXIMUM_SOURCE_LIMIT
            )
            TransactionError.SwapWeeklyLimitExceeded -> Pair(
                getString(
                    com.blockchain.stringResources.R.string.trading_weekly_limit_exceeded,
                    getActionStringResource(state.action)
                ),
                OVER_MAXIMUM_SOURCE_LIMIT
            )
            TransactionError.SwapYearlyLimitExceeded -> Pair(
                getString(
                    com.blockchain.stringResources.R.string.trading_yearly_limit_exceeded,
                    getActionStringResource(state.action)
                ),
                OVER_MAXIMUM_SOURCE_LIMIT
            )
            TransactionError.InvalidCryptoAddress -> {
                Pair(getString(com.blockchain.stringResources.R.string.trading_invalid_address), INVALID_ADDRESS)
            }
            TransactionError.InvalidCryptoCurrency -> {
                Pair(
                    getString(com.blockchain.stringResources.R.string.trading_invalid_currency),
                    INVALID_CRYPTO_CURRENCY
                )
            }
            TransactionError.InvalidFiatCurrency -> {
                Pair(getString(com.blockchain.stringResources.R.string.trading_invalid_fiat), INVALID_FIAT_CURRENCY)
            }
            TransactionError.OrderDirectionDisabled -> Pair(
                getString(com.blockchain.stringResources.R.string.trading_direction_disabled),
                ORDER_DIRECTION_DISABLED
            )
            TransactionError.InvalidOrExpiredQuote -> Pair(
                getString(
                    com.blockchain.stringResources.R.string.trading_quote_invalid_or_expired
                ),
                INVALID_QUOTE
            )
            TransactionError.IneligibleForSwap -> Pair(
                getString(com.blockchain.stringResources.R.string.trading_ineligible_for_swap),
                INELIGIBLE
            )
            TransactionError.InvalidDestinationAmount -> Pair(
                getString(
                    com.blockchain.stringResources.R.string.trading_invalid_destination_amount
                ),
                INVALID_AMOUNT
            )
            TransactionError.InvalidPostcode -> Pair(
                getString(
                    com.blockchain.stringResources.R.string.address_verification_postcode_error
                ),
                INVALID_POSTCODE
            )
            is TransactionError.ExecutionFailed -> Pair(
                getString(
                    com.blockchain.stringResources.R.string.executing_transaction_error,
                    state.sendingAsset.displayTicker
                ),
                EXECUTION_FAILED
            )
            is TransactionError.InternetConnectionError -> Pair(
                getString(
                    com.blockchain.stringResources.R.string.executing_connection_error
                ),
                INTERNET_CONNECTION_ERROR
            )
            is TransactionError.HttpError ->
                Pair(
                    getString(
                        com.blockchain.stringResources.R.string.common_http_error_with_new_line_message,
                        error.nabuApiException.getErrorDescription()
                    ),
                    NABU_ERROR
                )
            TransactionError.InvalidDomainAddress -> Pair(
                getString(
                    com.blockchain.stringResources.R.string.invalid_domain_address
                ),
                INVALID_ADDRESS
            )
            TransactionError.TransactionDenied -> Pair(
                getString(com.blockchain.stringResources.R.string.transaction_denied),
                UNKNOWN_ERROR
            )
            is TransactionError.FiatDepositError -> Pair(
                customiser.transactionProgressExceptionTitle(state),
                error.errorCode
            )
            is TransactionError.SettlementRefreshRequired -> Pair(
                customiser.transactionProgressExceptionTitle(state),
                SettlementReason.REQUIRES_UPDATE.toString()
            )
            TransactionError.SettlementGenericError -> Pair(
                customiser.transactionProgressExceptionTitle(state),
                SettlementReason.GENERIC.toString()
            )
            TransactionError.SettlementInsufficientBalance -> Pair(
                customiser.transactionProgressExceptionTitle(state),
                SettlementReason.INSUFFICIENT_BALANCE.toString()
            )
            TransactionError.SettlementStaleBalance -> Pair(
                customiser.transactionProgressExceptionTitle(state),
                SettlementReason.STALE_BALANCE.toString()
            )
        }

        // Making it uppercase to match "BUY" in ClientErrorAnalytics
        sendAnalyticsEvent(
            analyticsPair = pair,
            action = state.action.name.uppercase(Locale.getDefault()),
            nabuApiException = nabuApiException,
            errorDescription = error.message
        )
    }

    private fun getActionStringResource(action: AssetAction): String =
        resources.getString(
            when (action) {
                AssetAction.Send -> com.blockchain.stringResources.R.string.common_send
                AssetAction.FiatWithdraw,
                AssetAction.ActiveRewardsWithdraw,
                AssetAction.StakingWithdraw,
                AssetAction.InterestWithdraw -> com.blockchain.stringResources.R.string.common_withdraw
                AssetAction.Swap -> com.blockchain.stringResources.R.string.common_swap
                AssetAction.Sell -> com.blockchain.stringResources.R.string.common_sell
                AssetAction.Sign -> com.blockchain.stringResources.R.string.common_sign
                AssetAction.InterestDeposit,
                AssetAction.StakingDeposit,
                AssetAction.ActiveRewardsDeposit,
                AssetAction.FiatDeposit -> com.blockchain.stringResources.R.string.common_deposit
                AssetAction.ViewActivity -> com.blockchain.stringResources.R.string.common_activity
                AssetAction.Receive -> com.blockchain.stringResources.R.string.common_receive
                AssetAction.ViewStatement -> com.blockchain.stringResources.R.string.common_summary
                AssetAction.Buy -> com.blockchain.stringResources.R.string.common_buy
            }
        )

    private fun handleActionForErrorState(settlementErrorStateAction: SettlementErrorStateAction) {
        when (settlementErrorStateAction) {
            is SettlementErrorStateAction.RelinkBank -> {
                binding.txProgressView.setupPrimaryCta(
                    text = settlementErrorStateAction.message,
                    onClick = {
                        refreshBankResultLauncher.launch(
                            Pair(settlementErrorStateAction.bankAccountId, BankAuthSource.DEPOSIT)
                        )
                    }
                )
            }
            SettlementErrorStateAction.None -> {}
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PAYMENT_APPROVAL && resultCode == RESULT_CANCELED) {
            model.process(
                TransactionIntent.TransactionApprovalDenied
            )
        }
    }

    private fun collectStackTraceString(e: Throwable): String {
        var stackTraceString = ""
        var index = 0
        while (stackTraceString.length <= MAX_STACKTRACE_CHARS && index < e.stackTrace.size) {
            stackTraceString += "${e.stackTrace[index]}\n"
            index++
        }
        Timber.d("Sending trace to analytics: $stackTraceString")
        return stackTraceString
    }

    companion object {
        fun newInstance(): TransactionProgressFragment = TransactionProgressFragment()
        private const val PAYMENT_APPROVAL = 3974
        private const val MAX_STACKTRACE_CHARS = 400
    }
}
