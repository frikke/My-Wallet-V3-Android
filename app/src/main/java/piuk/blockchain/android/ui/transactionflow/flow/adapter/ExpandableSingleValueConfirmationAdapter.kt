package piuk.blockchain.android.ui.transactionflow.flow.adapter

import android.graphics.drawable.Animatable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.TxConfirmation
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visibleIf
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemCheckoutSingleValueExpandableBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.transactionflow.flow.ConfirmationPropertyKey
import piuk.blockchain.android.ui.transactionflow.flow.TxConfirmReadOnlyMapperCheckout
import piuk.blockchain.android.util.context
import piuk.blockchain.android.util.getResolvedColor

class ExpandableSingleValueConfirmationAdapter(private val mapper: TxConfirmReadOnlyMapperCheckout) :
    AdapterDelegate<TxConfirmationValue> {
    override fun isForViewType(items: List<TxConfirmationValue>, position: Int): Boolean {
        return items[position].confirmation == TxConfirmation.EXPANDABLE_SINGLE_VALUE_READ_ONLY
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        ExpandableSingleValueCheckoutItemViewHolder(
            ItemCheckoutSingleValueExpandableBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            mapper
        )

    override fun onBindViewHolder(
        items: List<TxConfirmationValue>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as ExpandableSingleValueCheckoutItemViewHolder).bind(
        items[position]
    )
}

private class ExpandableSingleValueCheckoutItemViewHolder(
    private val binding: ItemCheckoutSingleValueExpandableBinding,
    private val mapper: TxConfirmReadOnlyMapperCheckout
) : RecyclerView.ViewHolder(binding.root) {
    private var isExpanded = false

    init {
        with(binding) {
            arrowIcon.setOnClickListener {
                isExpanded = !isExpanded
                expandableItemValue.visibleIf { isExpanded }
                updateIcon()
                startAnimation()
            }
        }
    }

    fun bind(item: TxConfirmationValue) {
        with(binding) {
            expandableItemValue.gone()
            arrowIcon.setImageResource(R.drawable.collapse_animated)
            mapper.map(item).run {
                expandableItemValue.text = this[ConfirmationPropertyKey.TITLE] as String
                expandableItemLabel.text = this[ConfirmationPropertyKey.LABEL] as String
            }
            updateIcon()
        }
    }

    private fun updateIcon() {
        with(binding) {
            if (isExpanded) {
                arrowIcon.setImageResource(R.drawable.expand_animated)
                arrowIcon.setColorFilter(context.getResolvedColor(R.color.blue_600))
            } else {
                arrowIcon.setImageResource(R.drawable.collapse_animated)
                arrowIcon.setColorFilter(context.getResolvedColor(R.color.grey_600))
            }
        }
    }

    private fun startAnimation() {
        with(binding) {
            val arrow = arrowIcon.drawable as Animatable
            arrow.start()
        }
    }
}
