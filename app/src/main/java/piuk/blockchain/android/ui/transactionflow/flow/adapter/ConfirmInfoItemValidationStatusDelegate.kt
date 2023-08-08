package piuk.blockchain.android.ui.transactionflow.flow.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.TxConfirmation
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.coincore.ValidationState
import com.blockchain.componentlib.viewextensions.updateItemBackground
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.asAssetInfoOrThrow
import info.blockchain.balance.isLayer2Token
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemSendConfirmErrorNoticeBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.context

class ConfirmInfoItemValidationStatusDelegate<in T> :
    AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean {
        return (items[position] as? TxConfirmationValue)?.confirmation == TxConfirmation.ERROR_NOTICE
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        ViewHolder(
            ItemSendConfirmErrorNoticeBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            parent
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as ViewHolder).bind(
        items[position] as TxConfirmationValue.ErrorNotice,
        isFirstItemInList = position == 0,
        isLastItemInList = items.lastIndex == position
    )

    class ViewHolder(
        private val binding: ItemSendConfirmErrorNoticeBinding,
        private val parentView: ViewGroup
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: TxConfirmationValue.ErrorNotice,
            isFirstItemInList: Boolean,
            isLastItemInList: Boolean
        ) {
            if (parentView is RecyclerView) {
                parentView.smoothScrollToPosition(parentView.adapter!!.itemCount - 1)
            }

            with(binding) {
                root.updateItemBackground(isFirstItemInList, isLastItemInList)
                errorMsg.text = item.toText(context)
            }
        }

        // By the time we are on the confirmation screen most of these possible error should have been
        // filtered out. A few remain possible, because BE failures or BitPay invoices, thus:
        @SuppressLint("StringFormatInvalid")
        private fun TxConfirmationValue.ErrorNotice.toText(ctx: Context) =
            when (this.status) {
                ValidationState.CAN_EXECUTE -> throw IllegalStateException("Displaying OK in error status")
                ValidationState.UNINITIALISED -> throw IllegalStateException("Displaying OK in error status")
                ValidationState.INSUFFICIENT_FUNDS -> ctx.getString(
                    com.blockchain.stringResources.R.string.confirm_status_msg_insufficient_funds
                )
                ValidationState.INSUFFICIENT_GAS -> ctx.getString(
                    com.blockchain.stringResources.R.string.confirm_status_msg_insufficient_gas,
                    this.money?.currency?.asAssetInfoOrThrow()?.let { asset ->
                        asset.takeIf { it.isLayer2Token }?.coinNetwork?.networkTicker ?: asset.displayTicker
                    } ?: CryptoCurrency.ETHER.displayTicker
                )
                ValidationState.OPTION_INVALID -> ctx.getString(
                    com.blockchain.stringResources.R.string.confirm_status_msg_option_invalid
                )
                ValidationState.MEMO_INVALID -> ctx.getString(
                    com.blockchain.stringResources.R.string.confirm_status_memo_invalid
                )
                ValidationState.INVOICE_EXPIRED -> ctx.getString(
                    com.blockchain.stringResources.R.string.confirm_status_msg_invoice_expired
                )
                ValidationState.UNDER_MIN_LIMIT -> {
                    this.money?.toStringWithSymbol()?.let {
                        ctx.getString(com.blockchain.stringResources.R.string.min_with_value, it)
                    } ?: ctx.getString(com.blockchain.stringResources.R.string.fee_options_sat_byte_min_error)
                }
                ValidationState.INVALID_AMOUNT -> ctx.getString(
                    com.blockchain.stringResources.R.string.fee_options_invalid_amount
                )
                ValidationState.HAS_TX_IN_FLIGHT -> ctx.getString(
                    com.blockchain.stringResources.R.string.send_error_tx_in_flight
                )
                else -> ctx.getString(com.blockchain.stringResources.R.string.confirm_status_msg_unexpected_error)
            }
    }
}
