package piuk.blockchain.android.ui.transactionflow.flow.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.TxConfirmation
import com.blockchain.coincore.TxConfirmationValue
import piuk.blockchain.android.databinding.ItemCheckoutCtaInfoBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.transactionflow.flow.ConfirmationPropertyKey
import piuk.blockchain.android.ui.transactionflow.flow.TxConfirmReadOnlyMapperCheckout

class ReadMoreDisclaimerDelegate(
    private val mapper: TxConfirmReadOnlyMapperCheckout,
    private val onTooltipClicked: (TxConfirmationValue) -> Unit,
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
        onTooltipClicked
    )
}

private class ReadMoreDisclaimerViewHolder(
    val binding: ItemCheckoutCtaInfoBinding,
    val mapper: TxConfirmReadOnlyMapperCheckout
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: TxConfirmationValue, onTooltipClicked: (TxConfirmationValue) -> Unit) {
        mapper.map(item).let {
            with(binding) {
                with(binding) {
                    infoText.text = it[ConfirmationPropertyKey.LABEL] as String
                    ctaButton.apply {
                        text = it[ConfirmationPropertyKey.CTA] as String
                        onClick = { onTooltipClicked(item) }
                    }
                }
            }
        }
    }
}
