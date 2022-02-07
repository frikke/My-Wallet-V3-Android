package piuk.blockchain.android.ui.customviews.account

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.componentlib.viewextensions.visible
import info.blockchain.balance.Money
import piuk.blockchain.android.databinding.FundsLockedSummaryItemBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate

class AccountLocksDelegate(
    private val onExtraInfoAccountClicked: (AccountLocks) -> Unit
) : AdapterDelegate<Any> {

    override fun isForViewType(items: List<Any>, position: Int): Boolean =
        items[position] is AccountLocks

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        LocksViewHolder(
            FundsLockedSummaryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onExtraInfoAccountClicked
        )

    override fun onBindViewHolder(
        items: List<Any>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as LocksViewHolder).bind(items[position] as AccountLocks)
}

private class LocksViewHolder(
    private val binding: FundsLockedSummaryItemBinding,
    private val onExtraInfoAccountClicked: (AccountLocks) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(accountLocks: AccountLocks) {
        if (accountLocks.fundsLocks != null && accountLocks.fundsLocks.locks.isNotEmpty()) {
            itemView.layoutParams = RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            with(binding) {
                val amount = accountLocks.fundsLocks.onHoldTotalAmount
                val total = if (amount.isPositive) amount else Money.zero(amount.currency)
                root.apply {
                    visible()
                    setOnClickListener { onExtraInfoAccountClicked(accountLocks) }
                }
                totalAmountLocked.text = total.toStringWithSymbol()
            }
        } else {
            itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
        }
    }
}
