package piuk.blockchain.android.ui.dashboard.coinview.accounts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.card.ButtonType
import com.blockchain.componentlib.card.CardButton
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ViewCoinviewRecurringBuyInfoBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.coinview.AssetDetailsItem

class RecurringBuyInfoItemDelegate(
    private val onCardClicked: () -> Unit
) : AdapterDelegate<AssetDetailsItem> {
    override fun isForViewType(items: List<AssetDetailsItem>, position: Int): Boolean =
        items[position] is AssetDetailsItem.RecurringBuyBanner

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        RecurringBuyInfoCardViewHolder(
            ViewCoinviewRecurringBuyInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(
        items: List<AssetDetailsItem>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as RecurringBuyInfoCardViewHolder).bind(onCardClicked)
}

private class RecurringBuyInfoCardViewHolder(
    val binding: ViewCoinviewRecurringBuyInfoBinding
) : RecyclerView.ViewHolder(binding.root as View) {

    fun bind(onCardClicked: () -> Unit) {
        binding.rbInfoCard.apply {
            isDismissable = false
            callToActionButton = CardButton(
                context.getString(R.string.learn_more),
                type = ButtonType.Minimal
            ) { onCardClicked() }
            title = context.getString(R.string.coinview_rb_card_title)
            subtitle = context.getString(R.string.coinview_rb_card_blurb)
            iconResource =
                ImageResource.LocalWithBackground(R.drawable.ic_tx_recurring_buy, R.color.blue_600, R.color.blue_200)
        }
    }
}
