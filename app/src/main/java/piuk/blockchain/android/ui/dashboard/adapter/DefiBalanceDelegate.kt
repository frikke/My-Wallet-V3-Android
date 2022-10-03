package piuk.blockchain.android.ui.dashboard.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.databinding.ItemDashboardDefiBalanceCardBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.model.DashboardItem
import piuk.blockchain.android.ui.dashboard.model.DefiBalanceState

class DefiBalanceDelegate : AdapterDelegate<DashboardItem> {
    override fun isForViewType(items: List<DashboardItem>, position: Int): Boolean =
        items[position] is DefiBalanceState

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        DefiBalanceViewHolder(
            binding = ItemDashboardDefiBalanceCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<DashboardItem>,
        position: Int,
        holder: RecyclerView.ViewHolder,
    ) = (holder as DefiBalanceViewHolder).bind(
        items[position] as DefiBalanceState,
    )
}

private class DefiBalanceViewHolder(
    private val binding: ItemDashboardDefiBalanceCardBinding,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(state: DefiBalanceState) {

        if (state.isLoading) {
            renderLoading()
        } else {
            renderLoaded(state)
        }
    }

    private fun renderLoaded(state: DefiBalanceState) {
        with(binding) {
            totalBalance.text = state.fiatBalance?.toStringWithSymbol().orEmpty()
        }
    }

    private fun renderLoading() {
        with(binding) {
            totalBalance.resetLoader()
        }
    }
}
