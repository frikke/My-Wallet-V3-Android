package piuk.blockchain.android.ui.dashboard.coinview.accounts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.componentlib.alert.AlertType
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ViewCoinviewItemErrorBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.coinview.AssetDetailsItem
import piuk.blockchain.android.util.context

class RecurringBuyErrorDelegate : AdapterDelegate<AssetDetailsItem> {
    override fun isForViewType(items: List<AssetDetailsItem>, position: Int): Boolean =
        items[position] is AssetDetailsItem.RecurringBuyError

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        RecurringBuyErrorCardViewHolder(
            ViewCoinviewItemErrorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<AssetDetailsItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as RecurringBuyErrorCardViewHolder).bind()
}

private class RecurringBuyErrorCardViewHolder(
    val binding: ViewCoinviewItemErrorBinding
) : RecyclerView.ViewHolder(binding.root as View) {

    fun bind() {
        with(binding) {
            itemLabel.title = context.getString(R.string.dashboard_recurring_buy_title)

            itemErrorCard.apply {
                isDismissable = false
                title = context.getString(R.string.coinview_recuring_buy_load_error_title)
                subtitle = context.getString(R.string.coinview_recuring_buy_load_error_subtitle)
                isBordered = true
                alertType = AlertType.Warning
            }
        }
    }
}
