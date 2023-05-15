package piuk.blockchain.android.ui.transactionflow.flow.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.TxConfirmation
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.componentlib.viewextensions.updateItemBackground
import piuk.blockchain.android.databinding.ItemCheckoutCtaInfoBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.transactionflow.flow.ConfirmationPropertyKey
import piuk.blockchain.android.ui.transactionflow.flow.TxConfirmReadOnlyMapperCheckout

class ReadMoreDisclaimerDelegate(
    private val mapper: TxConfirmReadOnlyMapperCheckout,
    private val onTooltipClicked: (TxConfirmationValue) -> Unit
) : AdapterDelegate<TxConfirmationValue> {
    override fun isForViewType(items: List<TxConfirmationValue>, position: Int): Boolean {
        return items[position].confirmation == TxConfirmation.DISCLAIMER_READ_MORE
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        ReadMoreDisclaimerViewHolder(
            ItemCheckoutCtaInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            mapper
        )

    override fun onBindViewHolder(
        items: List<TxConfirmationValue>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as ReadMoreDisclaimerViewHolder).bind(
        items[position],
        onTooltipClicked,
        isFirstItemInList = position == 0,
        isLastItemInList = items.lastIndex == position
    )
}

private class ReadMoreDisclaimerViewHolder(
    val binding: ItemCheckoutCtaInfoBinding,
    val mapper: TxConfirmReadOnlyMapperCheckout
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        item: TxConfirmationValue,
        onTooltipClicked: (TxConfirmationValue) -> Unit,
        isFirstItemInList: Boolean,
        isLastItemInList: Boolean
    ) {
        mapper.map(item).let {
            with(binding) {
                root.updateItemBackground(isFirstItemInList, isLastItemInList)

                infoText.text = it[ConfirmationPropertyKey.LABEL] as String
                ctaButton.apply {
                    text = it[ConfirmationPropertyKey.CTA] as String
                    onClick = { onTooltipClicked(item) }
                }
            }
        }
    }
}
