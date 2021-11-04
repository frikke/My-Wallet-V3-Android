package piuk.blockchain.android.ui.locks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.core.payments.model.FundsLock
import com.blockchain.utils.capitalizeFirstChar
import piuk.blockchain.android.databinding.LockItemBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate

class LockItemDelegate : AdapterDelegate<FundsLock> {

    override fun isForViewType(items: List<FundsLock>, position: Int): Boolean = true

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        LockItemViewHolder(
            LockItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<FundsLock>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as LockItemViewHolder).bind(items[position])
}

private class LockItemViewHolder(
    private val binding: LockItemBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(lock: FundsLock) {
        with(binding) {
            dateLock.text = "${lock.date.month.name.capitalizeFirstChar()} ${lock.date.dayOfMonth}"
            amountLock.text = lock.amount.toStringWithSymbol()
        }
    }
}
