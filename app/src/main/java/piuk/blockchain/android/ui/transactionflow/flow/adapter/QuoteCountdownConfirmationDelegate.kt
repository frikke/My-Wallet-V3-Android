package piuk.blockchain.android.ui.transactionflow.flow.adapter

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.TxConfirmation
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.componentlib.viewextensions.updateItemBackground
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemConfirmQuoteCountdownBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate

class QuoteCountdownConfirmationDelegate : AdapterDelegate<TxConfirmationValue> {
    override fun isForViewType(items: List<TxConfirmationValue>, position: Int): Boolean =
        items[position].confirmation == TxConfirmation.QUOTE_COUNTDOWN

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        QuoteCountdownViewHolder(
            ItemConfirmQuoteCountdownBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(items: List<TxConfirmationValue>, position: Int, holder: RecyclerView.ViewHolder) =
        (holder as QuoteCountdownViewHolder).bind(
            items[position] as TxConfirmationValue.QuoteCountDown,
            isFirstItemInList = position == 0,
            isLastItemInList = items.lastIndex == position
        )
}

private class QuoteCountdownViewHolder(
    private val binding: ItemConfirmQuoteCountdownBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        item: TxConfirmationValue.QuoteCountDown,
        isFirstItemInList: Boolean,
        isLastItemInList: Boolean
    ) {
        with(binding) {
            root.updateItemBackground(isFirstItemInList, isLastItemInList)
            val formattedTime = DateUtils.formatElapsedTime(item.pricedQuote.remainingSeconds.toLong())
            confirmationCountdownProgress.apply {
                progress = item.pricedQuote.remainingPercentage
                text = context.getString(
                    com.blockchain.stringResources.R.string.tx_confirmation_countdown,
                    formattedTime
                )
            }
        }
    }
}
