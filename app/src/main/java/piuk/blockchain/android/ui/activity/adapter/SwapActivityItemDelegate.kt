package piuk.blockchain.android.ui.activity.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.TradeActivitySummaryItem
import com.blockchain.utils.toFormattedDate
import info.blockchain.balance.Currency
import info.blockchain.balance.CurrencyType
import java.util.Date
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.DialogActivitiesTxItemBinding
import piuk.blockchain.android.ui.activity.ActivityType
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.context
import piuk.blockchain.android.util.getResolvedColor
import piuk.blockchain.android.util.setAssetIconColoursWithTint
import piuk.blockchain.android.util.setTransactionHasFailed
import piuk.blockchain.android.util.setTransactionIsConfirming

class SwapActivityItemDelegate<in T>(
    private val onItemClicked: (Currency, String, ActivityType) -> Unit // crypto, txID, type
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        (items[position] as? TradeActivitySummaryItem)?.isSwapPair() ?: false

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        SwapActivityItemViewHolder(
            DialogActivitiesTxItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as SwapActivityItemViewHolder).bind(
        items[position] as TradeActivitySummaryItem,
        onItemClicked
    )
}

private fun TradeActivitySummaryItem.isSwapPair(): Boolean = currencyPair.source.type == CurrencyType.CRYPTO &&
    currencyPair.destination.type == CurrencyType.CRYPTO && currencyPair.source != currencyPair.destination

private class SwapActivityItemViewHolder(
    private val binding: DialogActivitiesTxItemBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        tx: TradeActivitySummaryItem,
        onAccountClicked: (Currency, String, ActivityType) -> Unit
    ) {
        val pair = tx.currencyPair
        with(binding) {
            statusDate.text = Date(tx.timeStampMs).toFormattedDate()
            txType.text = context.resources.getString(
                R.string.tx_title_swapped,
                pair.source.displayTicker,
                pair.destination.displayTicker
            )
            when {
                tx.state.isPending -> icon.setTransactionIsConfirming()
                tx.state.hasFailed -> icon.setTransactionHasFailed()
                else -> {
                    icon.setImageResource(R.drawable.ic_tx_swap)
                    icon.setAssetIconColoursWithTint(pair.source)
                }
            }
            txRoot.setOnClickListener {
                onAccountClicked(
                    pair.source, tx.txId, ActivityType.SWAP
                )
            }

            setTextColours(tx.state.isPending)

            assetBalanceCrypto.text = tx.value.toStringWithSymbol()
            assetBalanceFiat.text = tx.fiatValue.toStringWithSymbol()
        }
    }

    private fun setTextColours(isPending: Boolean) {
        with(binding) {
            if (!isPending) {
                txType.setTextColor(context.getResolvedColor(R.color.black))
                statusDate.setTextColor(context.getResolvedColor(R.color.grey_600))
                assetBalanceFiat.setTextColor(context.getResolvedColor(R.color.grey_600))
                assetBalanceCrypto.setTextColor(context.getResolvedColor(R.color.black))
            } else {
                txType.setTextColor(context.getResolvedColor(R.color.grey_400))
                statusDate.setTextColor(context.getResolvedColor(R.color.grey_400))
                assetBalanceFiat.setTextColor(context.getResolvedColor(R.color.grey_400))
                assetBalanceFiat.setTextColor(context.getResolvedColor(R.color.grey_400))
            }
        }
    }
}
