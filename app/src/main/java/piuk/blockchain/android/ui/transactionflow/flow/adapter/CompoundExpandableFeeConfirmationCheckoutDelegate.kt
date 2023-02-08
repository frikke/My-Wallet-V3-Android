package piuk.blockchain.android.ui.transactionflow.flow.adapter

import android.graphics.drawable.Animatable
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.FeeInfo
import com.blockchain.coincore.TxConfirmation
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.componentlib.viewextensions.goneIf
import com.blockchain.componentlib.viewextensions.updateItemBackground
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.presentation.getResolvedColor
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.isLayer2Token
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemFeeCheckoutCompoundExpandableInfoBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.transactionflow.flow.ConfirmationPropertyKey
import piuk.blockchain.android.ui.transactionflow.flow.TxConfirmReadOnlyMapperCheckout
import piuk.blockchain.android.util.context

class CompoundExpandableFeeConfirmationCheckoutDelegate(private val mapper: TxConfirmReadOnlyMapperCheckout) :
    AdapterDelegate<TxConfirmationValue> {
    override fun isForViewType(items: List<TxConfirmationValue>, position: Int): Boolean {
        return items[position].confirmation == TxConfirmation.COMPOUND_EXPANDABLE_READ_ONLY
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        CompoundExpandableFeeConfirmationCheckoutDelegateItemViewHolder(
            ItemFeeCheckoutCompoundExpandableInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            mapper
        )

    override fun onBindViewHolder(
        items: List<TxConfirmationValue>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as CompoundExpandableFeeConfirmationCheckoutDelegateItemViewHolder).bind(
        items[position],
        isFirstItemInList = position == 0,
        isLastItemInList = items.lastIndex == position
    )
}

private class CompoundExpandableFeeConfirmationCheckoutDelegateItemViewHolder(
    val binding: ItemFeeCheckoutCompoundExpandableInfoBinding,
    private val mapper: TxConfirmReadOnlyMapperCheckout
) : RecyclerView.ViewHolder(binding.root) {
    private var isExpanded = false

    init {
        with(binding) {
            compoundItemNote.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    fun bind(item: TxConfirmationValue, isFirstItemInList: Boolean, isLastItemInList: Boolean) {
        with(binding) {
            root.updateItemBackground(isFirstItemInList, isLastItemInList)

            mapper.map(item).run {
                compoundItemLabel.text = this[ConfirmationPropertyKey.LABEL] as String
                compoundItemTitle.text = this[ConfirmationPropertyKey.TITLE] as String
                compoundItemNote.setText(
                    this[ConfirmationPropertyKey.LINKED_NOTE] as SpannableStringBuilder, TextView.BufferType.SPANNABLE
                )

                val hasSendingFee = this.containsKey(ConfirmationPropertyKey.FEE_ITEM_SENDING)
                if (hasSendingFee) {
                    val sendingItem = this[ConfirmationPropertyKey.FEE_ITEM_SENDING] as FeeInfo
                    compoundItemSendingLabel.text = getFeeLabel(sendingItem)
                    compoundItemSendingTitle.text = sendingItem.feeAmount.toStringWithSymbol()
                    compoundItemSendingSubtitle.text = sendingItem.fiatAmount.toStringWithSymbol()
                }

                val hasReceivingFee = this.containsKey(ConfirmationPropertyKey.FEE_ITEM_RECEIVING)
                if (hasReceivingFee) {
                    val receivingItem = this[ConfirmationPropertyKey.FEE_ITEM_RECEIVING] as FeeInfo
                    compoundItemReceivingLabel.text = getFeeLabel(receivingItem)
                    compoundItemReceivingTitle.text = receivingItem.feeAmount.toStringWithSymbol()
                    compoundItemReceivingSubtitle.text = receivingItem.fiatAmount.toStringWithSymbol()
                }

                compoundItemIcon.goneIf { !hasSendingFee && !hasReceivingFee }

                compoundItemIcon.setOnClickListener {
                    isExpanded = !isExpanded
                    if (hasReceivingFee) {
                        compoundItemReceivingGroup.visibleIf { isExpanded }
                    }
                    if (hasSendingFee) {
                        compoundItemSendingGroup.visibleIf { isExpanded }
                    }
                    updateIcon()
                    startAnimation()
                }
            }
        }

        updateIcon()
    }

    private fun getFeeLabel(item: FeeInfo) =
        if (item.asset.isLayer2Token) {
            val network = item.l1EvmNetwork?.networkTicker ?: CryptoCurrency.ETHER.displayTicker
            context.getString(
                R.string.checkout_item_erc20_network_fee,
                network,
                item.asset.displayTicker
            )
        } else {
            context.getString(R.string.checkout_item_network_fee, item.asset.displayTicker)
        }

    private fun updateIcon() {
        with(binding) {
            if (isExpanded) {
                compoundItemIcon.setImageResource(R.drawable.expand_animated)
                compoundItemIcon.setColorFilter(context.getResolvedColor(R.color.blue_600))
            } else {
                compoundItemIcon.setImageResource(R.drawable.collapse_animated)
                compoundItemIcon.setColorFilter(context.getResolvedColor(R.color.grey_600))
            }
        }
    }

    private fun startAnimation() {
        with(binding) {
            val arrow = compoundItemIcon.drawable as Animatable
            arrow.start()
        }
    }
}
