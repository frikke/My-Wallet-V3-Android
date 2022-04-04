package piuk.blockchain.android.ui.interest.tbm.presentation.adapter

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.nabu.datamanagers.repositories.interest.IneligibilityReason
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Money
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemInterestDashboardAssetInfoBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.resources.AssetResources
import piuk.blockchain.android.util.context

class InterestDashboardAssetItem<in T>(
    private val assetResources: AssetResources,
    private val itemClicked: (AssetInfo, Boolean) -> Unit
) : AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean {
        val item = items[position] as InterestDashboardItem
        return item is InterestDashboardItem.InterestAssetInfoItem
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        InterestAssetItemViewHolder(
            ItemInterestDashboardAssetInfoBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as InterestAssetItemViewHolder).bind(
        assetResources,
        items[position] as InterestDashboardItem.InterestAssetInfoItem,
        itemClicked
    )
}

private class InterestAssetItemViewHolder(
    private val binding: ItemInterestDashboardAssetInfoBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        assetResources: AssetResources,
        item: InterestDashboardItem.InterestAssetInfoItem,
        itemClicked: (AssetInfo, Boolean) -> Unit
    ) {
        with(binding) {
            assetResources.loadAssetIcon(itemInterestAssetIcon, item.assetInterestInfo.assetInfo)
            itemInterestAssetTitle.text = item.assetInterestInfo.assetInfo.name

            itemInterestAccBalanceTitle.text =
                context.getString(
                    R.string.rewards_dashboard_item_balance_title, item.assetInterestInfo.assetInfo.displayTicker
                )
        }

        if (item.assetInterestInfo.assetInterestDetail == null) {
            showDisabledState()
        } else {
            showInterestDetails(item, itemClicked)
        }
    }

    private fun showDisabledState() {
        with(binding) {
            itemInterestCta.isEnabled = false
            itemInterestCta.text = context.getString(R.string.rewards_dashboard_item_action_earn)
            itemInterestExplainer.visible()
            itemInterestExplainer.text = context.getString(R.string.rewards_item_issue_other)
        }
    }

    private fun showInterestDetails(
        item: InterestDashboardItem.InterestAssetInfoItem,
        itemClicked: (AssetInfo, Boolean) -> Unit
    ) {
        with(binding) {
            itemInterestAccEarnedLabel.text =
                item.assetInterestInfo.assetInterestDetail!!.totalInterest.toStringWithSymbol()

            itemInterestAccBalanceLabel.text =
                item.assetInterestInfo.assetInterestDetail.totalBalance.toStringWithSymbol()

            setDisabledExplanation(item.assetInterestInfo.assetInterestDetail.ineligibilityReason)

            setCta(item, itemClicked)

            setInterestInfo(item)
        }
    }

    private fun ItemInterestDashboardAssetInfoBinding.setCta(
        item: InterestDashboardItem.InterestAssetInfoItem,
        itemClicked: (AssetInfo, Boolean) -> Unit
    ) {
        itemInterestCta.isEnabled =
            (item.isKycGold && item.assetInterestInfo.assetInterestDetail!!.eligible) || item.assetInterestInfo.assetInterestDetail!!.totalBalance.isPositive

        itemInterestCta.text = if (item.assetInterestInfo.assetInterestDetail!!.totalBalance.isPositive) {
            context.getString(R.string.rewards_dashboard_item_action_view)
        } else {
            context.getString(R.string.rewards_dashboard_item_action_earn)
        }

        itemInterestCta.setOnClickListener {
            itemClicked(
                item.assetInterestInfo.assetInfo, item.assetInterestInfo.assetInterestDetail!!.totalBalance.isPositive
            )
        }
    }

    private fun ItemInterestDashboardAssetInfoBinding.setDisabledExplanation(ineligibilityReason: IneligibilityReason) {
        itemInterestExplainer.text = context.getString(
            when (ineligibilityReason) {
                IneligibilityReason.REGION -> R.string.rewards_item_issue_region
                IneligibilityReason.KYC_TIER -> R.string.rewards_item_issue_kyc
                IneligibilityReason.NONE -> R.string.empty
                else -> R.string.rewards_item_issue_other
            }
        )

        itemInterestExplainer.visibleIf { ineligibilityReason != IneligibilityReason.NONE }
    }

    private fun ItemInterestDashboardAssetInfoBinding.setInterestInfo(
        item: InterestDashboardItem.InterestAssetInfoItem,
    ) {
        val rateIntro = context.getString(R.string.rewards_dashboard_item_rate_1)
        val rateInfo = "${item.assetInterestInfo.assetInterestDetail!!.rate}%"
        val rateOutro =
            context.getString(R.string.rewards_dashboard_item_rate_2, item.assetInterestInfo.assetInfo.displayTicker)

        val sb = SpannableStringBuilder()
            .append(rateIntro)
            .append(rateInfo)
            .append(rateOutro)
        sb.setSpan(
            StyleSpan(Typeface.BOLD), rateIntro.length,
            rateIntro.length + rateInfo.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        itemInterestInfoText.setText(sb, TextView.BufferType.SPANNABLE)
    }

    private data class InterestDetails(
        val balance: Money,
        val totalInterest: Money,
        val interestRate: Double,
        val available: Boolean,
        val disabledReason: IneligibilityReason
    )
}
