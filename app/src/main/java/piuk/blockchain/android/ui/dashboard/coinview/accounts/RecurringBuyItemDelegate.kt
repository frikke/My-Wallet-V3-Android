package piuk.blockchain.android.ui.dashboard.coinview.accounts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.core.recurringbuy.domain.RecurringBuy
import com.blockchain.core.recurringbuy.domain.RecurringBuyState
import com.blockchain.utils.toFormattedDateWithoutYear
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ViewCoinviewRecurringBuyBinding
import piuk.blockchain.android.simplebuy.toHumanReadableRecurringBuy
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.coinview.AssetDetailsItem

class RecurringBuyItemDelegate(
    private val onRecurringBuyClicked: (RecurringBuy) -> Unit
) : AdapterDelegate<AssetDetailsItem> {
    override fun isForViewType(items: List<AssetDetailsItem>, position: Int): Boolean =
        items[position] is AssetDetailsItem.RecurringBuyInfo

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        RecurringBuyViewHolder(
            ViewCoinviewRecurringBuyBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onRecurringBuyClicked
        )

    override fun onBindViewHolder(
        items: List<AssetDetailsItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as RecurringBuyViewHolder).bind(
        items[position] as AssetDetailsItem.RecurringBuyInfo,
        items.indexOfFirst { it is AssetDetailsItem.RecurringBuyInfo } == position
    )
}

private class RecurringBuyViewHolder(
    private val binding: ViewCoinviewRecurringBuyBinding,
    private val onRecurringBuyClicked: (RecurringBuy) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: AssetDetailsItem.RecurringBuyInfo, isFirstItemOfCategory: Boolean) {
        with(binding) {
            rbsLabel.apply {
                visibleIf { isFirstItemOfCategory }
                title = context.getString(R.string.dashboard_recurring_buy_title)
            }

            recurringBuyDetails.apply {
                startImageResource =
                    ImageResource.LocalWithBackgroundAndExternalResources(
                        R.drawable.ic_tx_rb, item.recurringBuy.asset.colour, "#FFFFFF", 1f
                    )

                primaryText = context.getString(
                    R.string.dashboard_recurring_buy_item_title_1,
                    item.recurringBuy.amount.toStringWithSymbol(),
                    item.recurringBuy.recurringBuyFrequency.toHumanReadableRecurringBuy(context)
                )
                secondaryText = if (item.recurringBuy.state == RecurringBuyState.ACTIVE) {
                    context.getString(
                        R.string.dashboard_recurring_buy_item_label,
                        item.recurringBuy.nextPaymentDate.toFormattedDateWithoutYear()
                    )
                } else {
                    context.getString(R.string.dashboard_recurring_buy_item_label_error)
                }
                onClick = {
                    onRecurringBuyClicked(item.recurringBuy)
                }
            }
        }

        binding.root.setOnClickListener { onRecurringBuyClicked(item.recurringBuy) }
    }
}
