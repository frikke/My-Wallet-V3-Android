package piuk.blockchain.android.ui.dashboard.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import info.blockchain.balance.FiatValue
import piuk.blockchain.android.databinding.FundsLockedSummaryItemBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.model.Locks
import piuk.blockchain.android.util.visible

class FundsLockedDelegate(
    private val onHoldAmountClicked: (Locks) -> Unit
) : AdapterDelegate<Any> {

    override fun isForViewType(items: List<Any>, position: Int): Boolean =
        items[position] is Locks

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        FundsLockedViewHolder(
            FundsLockedSummaryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onHoldAmountClicked
        )

    override fun onBindViewHolder(
        items: List<Any>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as FundsLockedViewHolder).bind(items[position] as Locks)
}

private class FundsLockedViewHolder(
    private val binding: FundsLockedSummaryItemBinding,
    private val onHoldAmountClicked: (Locks) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(locks: Locks) {
        if (locks.fundsLocks != null) {
            with(binding) {
                val amount = locks.fundsLocks.onHoldTotalAmount
                val total = if (amount.isPositive) amount else FiatValue.zero(amount.currencyCode)
                root.apply {
                    visible()
                    setOnClickListener { onHoldAmountClicked(locks) }
                }
                totalAmountLocked.text = total.toStringWithSymbol()
            }
        }
    }
}