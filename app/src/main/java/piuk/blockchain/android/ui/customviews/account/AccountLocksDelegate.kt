package piuk.blockchain.android.ui.customviews.account

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import info.blockchain.balance.FiatValue
import piuk.blockchain.android.databinding.FundsLockedSummaryItemBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible

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
            with(binding) {
                val amount = accountLocks.fundsLocks.onHoldTotalAmount
                val total = if (amount.isPositive) amount else FiatValue.zero(amount.currencyCode)
                root.apply {
                    visible()
                    setOnClickListener { onExtraInfoAccountClicked(accountLocks) }
                }
                totalAmountLocked.text = total.toStringWithSymbol()
            }
        } else {
            binding.root.gone()
        }
    }
}