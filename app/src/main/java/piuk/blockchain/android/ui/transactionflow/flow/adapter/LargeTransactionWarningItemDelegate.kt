package piuk.blockchain.android.ui.transactionflow.flow.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.TxConfirmation
import com.blockchain.coincore.TxConfirmationValue
import com.blockchain.componentlib.viewextensions.updateItemBackground
import piuk.blockchain.android.databinding.ItemSendLargeTxConfirmItemBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel

class LargeTransactionWarningItemDelegate<in T>(
    private val model: TransactionModel
) : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean =
        (items[position] as? TxConfirmationValue)?.confirmation == TxConfirmation.LARGE_TRANSACTION_WARNING

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        LargeTransactionViewHolder(
            ItemSendLargeTxConfirmItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    @Suppress("UNCHECKED_CAST")
    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as LargeTransactionViewHolder).bind(
        items[position] as TxConfirmationValue.TxBooleanConfirmation<Unit>,
        model,
        isFirstItemInList = position == 0,
        isLastItemInList = items.lastIndex == position
    )
}

private class LargeTransactionViewHolder(private val binding: ItemSendLargeTxConfirmItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(
        item: TxConfirmationValue.TxBooleanConfirmation<Unit>,
        model: TransactionModel,
        isFirstItemInList: Boolean,
        isLastItemInList: Boolean
    ) {
        with(binding) {
            root.updateItemBackground(isFirstItemInList, isLastItemInList)

            with(confirmCheckbox) {
                isChecked = item.value
                setOnCheckedChangeListener { _, isChecked ->
                    model.process(TransactionIntent.ModifyTxOption(item.copy(value = isChecked)))
                }
            }
        }
    }
}
