package com.blockchain.presentation.customviews.kyc

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.graphics.ColorFilter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.common.R
import com.blockchain.common.databinding.ItemKycUpgradeNowBasicBinding
import com.blockchain.common.databinding.ItemKycUpgradeNowVerifiedBinding
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.tag.TagType
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.theme.White
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.goneIf
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.domain.eligibility.model.TransactionsLimit

sealed class ViewPagerItem(
    val tab: KycUpgradeNowSheet.ViewPagerTab
) {
    data class Basic(
        val isBasicApproved: Boolean,
        val transactionsLimit: TransactionsLimit
    ) : ViewPagerItem(KycUpgradeNowSheet.ViewPagerTab.BASIC)

    object Verified : ViewPagerItem(KycUpgradeNowSheet.ViewPagerTab.VERIFIED)
}

class KycCtaViewPagerAdapter(
    private val basicClicked: () -> Unit,
    private val verifyClicked: () -> Unit
) : ListAdapter<ViewPagerItem, RecyclerView.ViewHolder>(DIFF_UTIL) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is ViewPagerItem.Basic -> R.layout.item_kyc_upgrade_now_basic
        ViewPagerItem.Verified -> R.layout.item_kyc_upgrade_now_verified
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = when (viewType) {
        R.layout.item_kyc_upgrade_now_basic ->
            KycBasicVH(
                ItemKycUpgradeNowBasicBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                basicClicked
            )
        R.layout.item_kyc_upgrade_now_verified -> KycVerifiedVH(
            ItemKycUpgradeNowVerifiedBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            verifyClicked
        )
        else -> throw IllegalStateException()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) =
        if (holder is KycBasicVH && payloads.isNotEmpty()) {
            payloads.forEach {
                if (it is IsBasicApprovedChangedPayload) holder.isBasicApprovedChanged(it.isBasicApproved)
            }
        } else onBindViewHolder(holder, position)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = when (holder) {
        is KycBasicVH -> holder.bind(getItem(position) as ViewPagerItem.Basic)
        else -> (holder as KycVerifiedVH).bind()
    }

    class KycBasicVH(
        private val binding: ItemKycUpgradeNowBasicBinding,
        private val basicClicked: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun isBasicApprovedChanged(isBasicApproved: Boolean) = with(binding) {
            rowTier.apply {
                endTag = TagViewState(
                    context.getString(
                        if (isBasicApproved) R.string.kyc_upgrade_now_basic_active
                        else R.string.kyc_upgrade_now_basic_limited_access
                    ),
                    TagType.InfoAlt()
                )
            }
            ctaButton.goneIf(isBasicApproved)
        }

        fun bind(item: ViewPagerItem.Basic): Unit = with(binding) {
            rowTier.apply {
                startImageResource = ImageResource.LocalWithBackground(
                    id = R.drawable.ic_verification_badge,
                    backgroundColor = White,
                    iconColor = Grey400,
                    alpha = 0f
                )
                primaryText = context.getString(R.string.kyc_upgrade_now_basic_level)
                endTag = TagViewState(
                    context.getString(
                        if (item.isBasicApproved) R.string.kyc_upgrade_now_basic_active
                        else R.string.kyc_upgrade_now_basic_limited_access
                    ),
                    TagType.InfoAlt()
                )
            }
            rowSendReceive.apply {
                startImageResource = ImageResource.LocalWithBackground(
                    id = R.drawable.ic_tx_sent,
                    backgroundColor = Blue600,
                    iconColor = Blue600
                )
                primaryText = context.getString(R.string.kyc_upgrade_now_basic_send_receive_title)
                secondaryText = context.getString(R.string.kyc_upgrade_now_basic_send_receive_description)
                endImageResource = ImageResource.Local(
                    id = R.drawable.ic_check_dark,
                    colorFilter = ColorFilter.tint(Blue600)
                )
            }
            rowSwap.apply {
                startImageResource = ImageResource.LocalWithBackground(
                    id = R.drawable.ic_tx_swap,
                    backgroundColor = Blue600,
                    iconColor = Blue600
                )
                primaryText = context.getString(R.string.kyc_upgrade_now_basic_swap_title)
                secondaryText = context.getString(R.string.kyc_upgrade_now_basic_swap_description)
                endImageResource = ImageResource.Local(
                    id = R.drawable.ic_check_dark,
                    colorFilter = ColorFilter.tint(Blue600)
                )
            }
            ctaButton.apply {
                text = context.getString(R.string.kyc_upgrade_now_basic_cta)
                onClick = basicClicked
            }
            ctaButton.goneIf { item.isBasicApproved }

            cardTransactionsLeft.apply {
                when (item.transactionsLimit) {
                    is TransactionsLimit.Limited -> {
                        visible()
                        setup(item.transactionsLimit.maxTransactionsCap, item.transactionsLimit.maxTransactionsLeft)
                    }
                    TransactionsLimit.Unlimited -> gone()
                }
            }
        }
    }

    class KycVerifiedVH(
        private val binding: ItemKycUpgradeNowVerifiedBinding,
        private val verifyClicked: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(): Unit = with(binding) {
            rowTier.apply {
                startImageResource = ImageResource.LocalWithBackground(
                    id = R.drawable.ic_verification_badge,
                    backgroundColor = White,
                    iconColor = Blue600,
                    alpha = 0f
                )
                primaryText = context.getString(R.string.kyc_upgrade_now_verified_level)
                endTag = TagViewState(
                    context.getString(R.string.kyc_upgrade_now_verified_full_access),
                    TagType.InfoAlt()
                )
            }
            rowSwap.apply {
                startImageResource = ImageResource.LocalWithBackground(
                    id = R.drawable.ic_tx_swap,
                    backgroundColor = Blue600,
                    iconColor = Blue600
                )
                primaryText = context.getString(R.string.kyc_upgrade_now_verified_swap_title)
                secondaryText = context.getString(R.string.kyc_upgrade_now_verified_swap_description)
                endImageResource = ImageResource.Local(
                    id = R.drawable.ic_check_dark,
                    colorFilter = ColorFilter.tint(Blue600)
                )
            }
            rowBuySell.apply {
                startImageResource = ImageResource.LocalWithBackground(
                    id = R.drawable.ic_tx_buy,
                    backgroundColor = Blue600,
                    iconColor = Blue600
                )
                primaryText = context.getString(R.string.kyc_upgrade_now_verified_buy_title)
                secondaryText = context.getString(R.string.kyc_upgrade_now_verified_buy_description)
                endImageResource = ImageResource.Local(
                    id = R.drawable.ic_check_dark,
                    colorFilter = ColorFilter.tint(Blue600)
                )
            }
            rowRewards.apply {
                startImageResource = ImageResource.LocalWithBackground(
                    id = R.drawable.ic_tx_interest,
                    backgroundColor = Blue600,
                    iconColor = Blue600
                )
                primaryText = context.getString(R.string.kyc_upgrade_now_verified_interest_title)
                secondaryText = context.getString(R.string.kyc_upgrade_now_verified_interest_description)
                endImageResource = ImageResource.Local(
                    id = R.drawable.ic_check_dark,
                    colorFilter = ColorFilter.tint(Blue600)
                )
            }
            ctaButton.apply {
                text = context.getString(R.string.kyc_upgrade_now_verified_cta)
                onClick = verifyClicked
            }
        }
    }

    companion object {
        private val DIFF_UTIL = object : DiffUtil.ItemCallback<ViewPagerItem>() {
            override fun areItemsTheSame(oldItem: ViewPagerItem, newItem: ViewPagerItem): Boolean =
                oldItem.tab == newItem.tab

            override fun areContentsTheSame(oldItem: ViewPagerItem, newItem: ViewPagerItem): Boolean =
                oldItem == newItem

            // This is needed in order to prevent the ViewPager from switching tabs automatically
            // when we update the isBasicApproved after we get the response from the Tier
            override fun getChangePayload(
                oldItem: ViewPagerItem,
                newItem: ViewPagerItem
            ): IsBasicApprovedChangedPayload? =
                if (
                    oldItem is ViewPagerItem.Basic &&
                    newItem is ViewPagerItem.Basic &&
                    oldItem.isBasicApproved != newItem.isBasicApproved
                ) IsBasicApprovedChangedPayload(newItem.isBasicApproved)
                else null
        }

        private data class IsBasicApprovedChangedPayload(val isBasicApproved: Boolean)
    }
}
