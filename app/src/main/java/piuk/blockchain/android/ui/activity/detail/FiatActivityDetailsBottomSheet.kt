package piuk.blockchain.android.ui.activity.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.blockchain.api.services.PaymentMethodDetails
import com.blockchain.coincore.FiatActivitySummaryItem
import com.blockchain.koin.scopedInject
import com.blockchain.nabu.datamanagers.TransactionState
import com.blockchain.nabu.datamanagers.TransactionType
import com.blockchain.utils.toFormattedString
import java.util.Date
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.DialogSheetActivityDetailsBinding
import piuk.blockchain.android.ui.activity.detail.adapter.FiatDetailsSheetAdapter
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.util.gone
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class FiatActivityDetailsBottomSheet : SlidingModalBottomDialog<DialogSheetActivityDetailsBinding>() {
    private val model: FiatActivityDetailsModel by scopedInject()
    private val fiatDetailsSheetAdapter = FiatDetailsSheetAdapter()
    private val currency: String by unsafeLazy {
        arguments?.getString(CURRENCY_KEY) ?: throw IllegalStateException("No currency  provided")
    }

    private val txHash: String by unsafeLazy {
        arguments?.getString(TX_HASH_KEY) ?: throw IllegalStateException("No tx  provided")
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSheetActivityDetailsBinding =
        DialogSheetActivityDetailsBinding.inflate(inflater, container, false)

    private fun initView(fiatActivitySummaryItem: FiatActivitySummaryItem) {
        with(binding) {

            title.text = if (fiatActivitySummaryItem.type == TransactionType.DEPOSIT) {
                getString(R.string.common_deposit)
            } else {
                getString(R.string.fiat_funds_detail_withdraw_title)
            }
            amount.text = fiatActivitySummaryItem.value.toStringWithSymbol()
            status.configureForState(fiatActivitySummaryItem.state)

            with(detailsList) {
                addItemDecoration(BlockchainListDividerDecor(requireContext()))
                adapter = fiatDetailsSheetAdapter
            }
            fiatDetailsSheetAdapter.items = getItemsForSummaryItem(fiatActivitySummaryItem)
        }
    }

    override fun initControls(binding: DialogSheetActivityDetailsBinding) {
        with(binding) {
            confirmationProgress.gone()
            confirmationLabel.gone()
            custodialTxButton.gone()
        }
    }

    override fun onStart() {
        super.onStart()
        parentFragment?.viewLifecycleOwner?.lifecycleScope?.launch {
            model.uiState.collect(::render)
        }
        model.findCachedItem(currency, txHash)
    }

    private fun render(state: FiatActivityDetailsViewState) {
        when {
            state.activityItem != null && state.paymentDetails != null ->
                fiatDetailsSheetAdapter.items = getItemsForSummaryItem(state.activityItem, state.paymentDetails)
            state.activityItem != null -> {
                initView((state.activityItem))
                model.loadPaymentDetails(state.activityItem)
            }
            state.errorMessage.isNotEmpty() -> {
                // TODO Add error handling
            }
            else -> {
                // TODO Add loading indicator
            }
        }
    }

    private fun TextView.configureForState(state: TransactionState) {
        when (state) {
            TransactionState.COMPLETED -> {
                text = getString(R.string.activity_details_completed)
                setBackgroundResource(R.drawable.bkgd_green_100_rounded)
                setTextColor(ContextCompat.getColor(context, R.color.green_600))
            }
            TransactionState.PENDING -> {
                text = getString(R.string.activity_details_label_pending)
                setBackgroundResource(R.drawable.bkgd_status_unconfirmed)
                setTextColor(ContextCompat.getColor(context, R.color.grey_800))
            }
            TransactionState.FAILED -> {
                text = getString(R.string.activity_details_label_failed)
                setBackgroundResource(R.drawable.bkgd_red_100_rounded)
                setTextColor(ContextCompat.getColor(context, R.color.red_600))
            }
            else -> {
                gone()
            }
        }
    }

    private fun getItemsForSummaryItem(
        item: FiatActivitySummaryItem,
        paymentDetails: PaymentMethodDetails? = null
    ): List<FiatDetailItem> =
        listOfNotNull(
            FiatDetailItem(
                key = getString(R.string.activity_details_buy_tx_id),
                value = item.txId
            ),
            FiatDetailItem(
                key = getString(R.string.date),
                value = Date(item.timeStampMs).toFormattedString()
            ),
            FiatDetailItem(
                key = if (item.type == TransactionType.DEPOSIT) {
                    getString(R.string.common_to)
                } else {
                    getString(R.string.common_from)
                },
                value = item.account.label
            ),
            FiatDetailItem(
                key = getString(R.string.amount),
                value = item.value.toStringWithSymbol()
            ),
            if (paymentDetails != null) {
                FiatDetailItem(
                    key = getString(R.string.activity_details_buy_payment_method),
                    value = paymentDetails.mapPaymentDetailsToString()
                )
            } else null
        )

    private fun PaymentMethodDetails.mapPaymentDetailsToString(): String {
        return if (label.isNullOrBlank()) {
            getString(R.string.checkout_funds_label_1, currency)
        } else {
            """${this.label} ${this.endDigits}"""
        }
    }

    companion object {
        private const val CURRENCY_KEY = "CURRENCY_KEY"
        private const val TX_HASH_KEY = "TX_HASH_KEY"

        fun newInstance(
            fiatCurrency: String,
            txHash: String
        ): FiatActivityDetailsBottomSheet {
            return FiatActivityDetailsBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(CURRENCY_KEY, fiatCurrency)
                    putString(TX_HASH_KEY, txHash)
                }
            }
        }
    }
}

data class FiatDetailItem(val key: String, val value: String)
