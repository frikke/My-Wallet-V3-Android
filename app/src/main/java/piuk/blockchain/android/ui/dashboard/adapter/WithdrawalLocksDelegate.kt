package piuk.blockchain.android.ui.dashboard.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.databinding.WithdrawalLockSummaryItemBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.model.Locks
import piuk.blockchain.android.util.visible

class WithdrawalLocksDelegate<in T>(
    private val onHoldAmountClicked: (Locks) -> Unit
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is Locks

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        LocksViewHolder(
            WithdrawalLockSummaryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onHoldAmountClicked
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as LocksViewHolder).bind(items[position] as Locks)
}

private class LocksViewHolder(
    private val binding: WithdrawalLockSummaryItemBinding,
    private val onHoldAmountClicked: (Locks) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(state: Locks) {
        with(binding) {
            state.withdrawalsLocks?.onHoldTotalAmount?.let { total ->
                if (total.isPositive) {
                    root.apply {
                        visible()
                        setOnClickListener { onHoldAmountClicked(state) }
                    }
                    totalAmountLocked.text = total.toStringWithSymbol()
                }
            }
        }
    }
}
