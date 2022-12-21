package piuk.blockchain.android.ui.transactionflow.flow.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.TxConfirmation
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.componentlib.viewextensions.updateItemBackgroundForSuperApp
import piuk.blockchain.android.databinding.ItemCheckoutClickableInfoBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.transactionflow.flow.ConfirmationPropertyKey
import piuk.blockchain.android.ui.transactionflow.flow.TxConfirmReadOnlyMapperCheckout

class TooltipConfirmationCheckoutDelegate(
    private val mapper: TxConfirmReadOnlyMapperCheckout,
    private val onTooltipClicked: (TxConfirmationValue) -> Unit,
) : AdapterDelegate<TxConfirmationValue> {
    override fun isForViewType(items: List<TxConfirmationValue>, position: Int): Boolean {
        return items[position].confirmation == TxConfirmation.SIMPLE_TOOLTIP
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        TooltipConfirmationCheckoutItemViewHolder(
            ItemCheckoutClickableInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            mapper
        )

    override fun onBindViewHolder(
        items: List<TxConfirmationValue>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as TooltipConfirmationCheckoutItemViewHolder).bind(
        items[position],
        onTooltipClicked,
        isFirstItemInList = position == 0,
        isLastItemInList = items.lastIndex == position
    )
}

private class TooltipConfirmationCheckoutItemViewHolder(
    val binding: ItemCheckoutClickableInfoBinding,
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
                root.updateItemBackgroundForSuperApp(isFirstItemInList, isLastItemInList)

                clickableItemLabel.text = it[ConfirmationPropertyKey.LABEL] as String
                clickableItemTitle.text = it[ConfirmationPropertyKey.TITLE] as String
                clickableItemLabel.setOnClickListener {
                    onTooltipClicked(item)
                }
            }
        }
    }
}
