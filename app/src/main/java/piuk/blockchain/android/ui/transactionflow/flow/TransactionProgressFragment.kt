package piuk.blockchain.android.ui.transactionflow.flow

import android.app.Activity.RESULT_CANCELED
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.api.NabuApiException
import com.blockchain.coincore.AssetAction
import com.blockchain.nabu.datamanagers.TransactionError
import java.util.Locale
import org.koin.android.ext.android.inject
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
import piuk.blockchain.android.ui.linkbank.BankAuthActivity
import piuk.blockchain.android.ui.linkbank.BankAuthSource
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.engine.TxExecutionStatus
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TransactionProgressCustomisations
import timber.log.Timber

class TransactionProgressFragment : TransactionFlowFragment<FragmentTxFlowInProgressBinding>() {

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTxFlowInProgressBinding =
        FragmentTxFlowInProgressBinding.inflate(inflater, container, false)

    private val customiser: TransactionProgressCustomisations by inject()
    private val MAX_STACKTRACE_CHARS = 400

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.txProgressView.onCtaClick(
            text = getString(R.string.common_ok)
        ) { activity.finish() }
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
            is TxExecutionStatus.InProgress -> binding.txProgressView.showTxInProgress(
                customiser.transactionProgressTitle(newState),
                customiser.transactionProgressMessage(newState)
            )
            is TxExecutionStatus.Completed -> {
                analyticsHooks.onTransactionSuccess(newState)
                binding.txProgressView.showTxSuccess(
                    customiser.transactionCompleteTitle(newState),
                    customiser.transactionCompleteMessage(newState),
                    customiser.transactionCompleteIcon(newState)
                )
            }
            is TxExecutionStatus.ApprovalRequired -> {
                binding.txProgressView.showTxInProgress(
                    customiser.transactionProgressTitle(newState),
                    customiser.transactionProgressMessage(newState)
                )
                model.process(TransactionIntent.ApprovalTriggered)
                startActivityForResult(
                    BankAuthActivity.newInstance(
                        newState.executionStatus.approvalData, BankAuthSource.DEPOSIT, requireContext()
                    ),
                    PAYMENT_APPROVAL
                )
                // dismiss()
            }
            is TxExecutionStatus.Error -> {
                analyticsHooks.onTransactionFailure(
                    newState, collectStackTraceString(newState.executionStatus.exception)
                )
                binding.txProgressView.showTxError(
                    customiser.transactionProgressExceptionMessage(newState),
                    getString(R.string.send_progress_error_subtitle)
                )
                logClientErrorToAnalytics(newState)
            }
            else -> {
                // do nothing
            }
        }
    }

    private fun sendAnalyticsEvent(
        analyticsPair: Pair<String, String>,
        action: String,
        nabuApiException: NabuApiException?,
    ) {
        analytics.logEvent(
            ClientErrorAnalytics.ClientLogError(
                nabuApiException = nabuApiException,
                error = analyticsPair.second,
                source = nabuApiException?.let { ClientErrorAnalytics.Companion.Source.NABU }
                    ?: ClientErrorAnalytics.Companion.Source.CLIENT,
                title = analyticsPair.first,
                action = action,
            )
        )
    }

    private fun logClientErrorToAnalytics(state: TransactionState) {
        require(state.executionStatus is TxExecutionStatus.Error)
        require(state.executionStatus.exception is TransactionError)
        val error = state.executionStatus.exception

        val nabuApiException = if (error is TransactionError.HttpError) {
            error.nabuApiException
        } else null

        val pair = when (error) {
            TransactionError.OrderLimitReached -> Pair(
                getString(
                    R.string.trading_order_limit, getActionStringResource(state.action)
                ),
                PENDING_ORDERS_LIMIT_REACHED
            )
            TransactionError.OrderNotCancelable -> Pair(
                getString(
                    R.string.trading_order_not_cancelable, getActionStringResource(state.action)
                ),
                ORDER_NOT_CANCELABLE
            )
            TransactionError.WithdrawalAlreadyPending -> Pair(
                getString(R.string.trading_pending_withdrawal),
                WITHDRAW_ALREADY_PENDING
            )
            TransactionError.WithdrawalBalanceLocked -> Pair(
                getString(R.string.trading_withdrawal_balance_locked),
                WITHDRAW_BALANCE_LOCKED
            )
            TransactionError.WithdrawalInsufficientFunds -> Pair(
                getString(R.string.trading_withdrawal_insufficient_funds),
                INSUFFICIENT_FUNDS
            )
            TransactionError.InternalServerError -> {
                Pair(
                    getString(R.string.trading_internal_server_error),
                    INTERNAL_SERVER_ERROR
                )
            }
            TransactionError.TradingTemporarilyDisabled -> Pair(
                getString(R.string.trading_service_temp_disabled),
                TRADING_DISABLED
            )
            TransactionError.InsufficientBalance -> Pair(
                getString(R.string.trading_insufficient_balance, getActionStringResource(state.action)),
                OVER_MAXIMUM_PERSONAL_LIMIT
            )
            TransactionError.OrderBelowMin -> Pair(
                getString(
                    R.string.trading_amount_below_min, getActionStringResource(state.action)
                ),
                BELOW_MINIMUM_LIMIT
            )
            TransactionError.OrderAboveMax -> Pair(
                getString(R.string.trading_amount_above_max, getActionStringResource(state.action)),
                OVER_MAXIMUM_SOURCE_LIMIT
            )
            TransactionError.SwapDailyLimitExceeded -> Pair(
                getString(
                    R.string.trading_daily_limit_exceeded, getActionStringResource(state.action)
                ),
                OVER_MAXIMUM_SOURCE_LIMIT
            )
            TransactionError.SwapWeeklyLimitExceeded -> Pair(
                getString(
                    R.string.trading_weekly_limit_exceeded, getActionStringResource(state.action)
                ),
                OVER_MAXIMUM_SOURCE_LIMIT
            )
            TransactionError.SwapYearlyLimitExceeded -> Pair(
                getString(
                    R.string.trading_yearly_limit_exceeded, getActionStringResource(state.action)
                ),
                OVER_MAXIMUM_SOURCE_LIMIT
            )
            TransactionError.InvalidCryptoAddress -> {
                Pair(getString(R.string.trading_invalid_address), INVALID_ADDRESS)
            }
            TransactionError.InvalidCryptoCurrency -> {
                Pair(getString(R.string.trading_invalid_currency), INVALID_CRYPTO_CURRENCY)
            }
            TransactionError.InvalidFiatCurrency -> {
                Pair(getString(R.string.trading_invalid_fiat), INVALID_FIAT_CURRENCY)
            }
            TransactionError.OrderDirectionDisabled -> Pair(
                getString(R.string.trading_direction_disabled),
                ORDER_DIRECTION_DISABLED
            )
            TransactionError.InvalidOrExpiredQuote -> Pair(
                getString(
                    R.string.trading_quote_invalid_or_expired
                ),
                INVALID_QUOTE
            )
            TransactionError.IneligibleForSwap -> Pair(
                getString(R.string.trading_ineligible_for_swap),
                INELIGIBLE
            )
            TransactionError.InvalidDestinationAmount -> Pair(
                getString(
                    R.string.trading_invalid_destination_amount
                ),
                INVALID_AMOUNT
            )
            TransactionError.InvalidPostcode -> Pair(
                getString(
                    R.string.kyc_postcode_error
                ),
                INVALID_POSTCODE
            )
            is TransactionError.ExecutionFailed -> Pair(
                getString(
                    R.string.executing_transaction_error, state.sendingAsset.displayTicker
                ),
                EXECUTION_FAILED
            )
            is TransactionError.InternetConnectionError -> Pair(
                getString(
                    R.string.executing_connection_error
                ),
                INTERNET_CONNECTION_ERROR
            )
            is TransactionError.HttpError ->
                Pair(
                    getString(
                        R.string.common_http_error_with_new_line_message,
                        error.nabuApiException.getErrorDescription(),
                    ),
                    NABU_ERROR
                )
            TransactionError.InvalidDomainAddress -> Pair(
                getString(
                    R.string.invalid_domain_address
                ),
                INVALID_ADDRESS
            )
            TransactionError.TransactionDenied -> Pair(getString(R.string.transaction_denied), UNKNOWN_ERROR)
        }

        // Making it uppercase to match "BUY" in ClientErrorAnalytics
        sendAnalyticsEvent(pair, state.action.name.uppercase(Locale.getDefault()), nabuApiException)
    }

    private fun getActionStringResource(action: AssetAction): String =
        resources.getString(
            when (action) {
                AssetAction.Send -> R.string.common_send
                AssetAction.Withdraw,
                AssetAction.InterestWithdraw -> R.string.common_withdraw
                AssetAction.Swap -> R.string.common_swap
                AssetAction.Sell -> R.string.common_sell
                AssetAction.Sign -> R.string.common_sign
                AssetAction.InterestDeposit,
                AssetAction.FiatDeposit -> R.string.common_deposit
                AssetAction.ViewActivity -> R.string.common_activity
                AssetAction.Receive -> R.string.common_receive
                AssetAction.ViewStatement -> R.string.common_summary
                AssetAction.Buy -> R.string.common_buy
            }
        )

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
    }
}
