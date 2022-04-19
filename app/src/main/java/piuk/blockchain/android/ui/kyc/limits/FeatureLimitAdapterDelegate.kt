package piuk.blockchain.android.ui.kyc.limits

import android.net.Uri
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.componentlib.viewextensions.invisibleIf
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.core.limits.Feature
import com.blockchain.core.limits.FeatureLimit
import com.blockchain.core.limits.TxLimitPeriod
import com.blockchain.nabu.Tier
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ItemFeatureLimitsCurrentTierBinding
import piuk.blockchain.android.databinding.ItemFeatureLimitsFeatureBinding
import piuk.blockchain.android.databinding.ItemFeatureLimitsFeatureHeaderBinding
import piuk.blockchain.android.databinding.ItemFeatureLimitsFooterBinding
import piuk.blockchain.android.databinding.ItemFeatureLimitsHeaderBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationDiffAdapter
import piuk.blockchain.android.urllinks.LIMITS_SUPPORT_CENTER
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.context

class FeatureLimitAdapterDelegate(
    private val onHeaderCtaClicked: (Header) -> Unit
) : DelegationDiffAdapter<KycLimitsItem>(AdapterDelegatesManager()) {
    init {
        with(delegatesManager) {
            addAdapterDelegate(HeaderItemDelegate(onHeaderCtaClicked))
            addAdapterDelegate(FeaturesHeaderItemDelegate())
            addAdapterDelegate(CurrentTierItemDelegate())
            addAdapterDelegate(FeatureWithLimitItemDelegate())
            addAdapterDelegate(FooterItemDelegate())
        }
    }
}

class HeaderItemDelegate(private val onHeaderCtaClicked: (Header) -> Unit) : AdapterDelegate<KycLimitsItem> {
    override fun isForViewType(items: List<KycLimitsItem>, position: Int): Boolean =
        items[position] is KycLimitsItem.HeaderItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        HeaderItemViewHolder(
            ItemFeatureLimitsHeaderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(items: List<KycLimitsItem>, position: Int, holder: RecyclerView.ViewHolder) =
        (holder as HeaderItemViewHolder).bind(items[position] as KycLimitsItem.HeaderItem, onHeaderCtaClicked)
}

private class HeaderItemViewHolder(
    val binding: ItemFeatureLimitsHeaderBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: KycLimitsItem.HeaderItem, onHeaderCtaClicked: (Header) -> Unit) {
        with(binding) {
            textTitle.text = when (item.header) {
                Header.NEW_KYC -> context.getString(R.string.feature_limits_header_bronze_title)
                Header.UPGRADE_TO_GOLD -> context.getString(R.string.feature_limits_header_silver_title)
                Header.MAX_TIER_REACHED,
                Header.HIDDEN -> null
            }
            textTitle.visibleIf { item.header != Header.MAX_TIER_REACHED }

            textDescription.text = when (item.header) {
                Header.NEW_KYC -> context.getString(R.string.feature_limits_header_bronze_description)
                Header.UPGRADE_TO_GOLD -> context.getString(R.string.feature_limits_header_silver_description)
                Header.MAX_TIER_REACHED -> context.getString(R.string.feature_limits_header_denied_description)
                Header.HIDDEN -> null
            }

            btnCta.text = when (item.header) {
                Header.NEW_KYC -> context.getString(R.string.feature_limits_header_bronze_cta)
                Header.UPGRADE_TO_GOLD -> context.getString(R.string.feature_limits_header_silver_cta)
                Header.MAX_TIER_REACHED -> null
                Header.HIDDEN -> null
            }
            btnCta.visibleIf { item.header != Header.MAX_TIER_REACHED }
            btnCta.setOnClickListener {
                onHeaderCtaClicked(item.header)
            }
        }
    }
}

class FeaturesHeaderItemDelegate : AdapterDelegate<KycLimitsItem> {
    override fun isForViewType(items: List<KycLimitsItem>, position: Int): Boolean =
        items[position] is KycLimitsItem.FeaturesHeaderItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        FeaturesHeaderItemViewHolder(
            ItemFeatureLimitsFeatureHeaderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(items: List<KycLimitsItem>, position: Int, holder: RecyclerView.ViewHolder) {
    }
}

private class FeaturesHeaderItemViewHolder(
    val binding: ItemFeatureLimitsFeatureHeaderBinding
) : RecyclerView.ViewHolder(binding.root)

class CurrentTierItemDelegate : AdapterDelegate<KycLimitsItem> {
    override fun isForViewType(items: List<KycLimitsItem>, position: Int): Boolean =
        items[position] is KycLimitsItem.CurrentTierItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        CurrentTierItemViewHolder(
            ItemFeatureLimitsCurrentTierBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(items: List<KycLimitsItem>, position: Int, holder: RecyclerView.ViewHolder) =
        (holder as CurrentTierItemViewHolder).bind(items[position] as KycLimitsItem.CurrentTierItem)
}

private class CurrentTierItemViewHolder(
    val binding: ItemFeatureLimitsCurrentTierBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: KycLimitsItem.CurrentTierItem) {
        with(binding) {
            iconTier.invisibleIf { item.tier == Tier.BRONZE }
            iconTier.setImageResource(
                when (item.tier) {
                    Tier.BRONZE -> R.drawable.ic_blockchain_logo
                    Tier.SILVER -> R.drawable.ic_blockchain_logo
                    Tier.GOLD -> R.drawable.ic_verification_badge
                }
            )

            textTier.text = when (item.tier) {
                Tier.BRONZE -> ""
                Tier.SILVER -> context.getString(R.string.feature_limits_silver_limits)
                Tier.GOLD -> context.getString(R.string.feature_limits_gold_limits)
            }
            textGoldTierDescription.visibleIf { item.tier == Tier.GOLD }
        }
    }
}

class FeatureWithLimitItemDelegate : AdapterDelegate<KycLimitsItem> {
    override fun isForViewType(items: List<KycLimitsItem>, position: Int): Boolean =
        items[position] is KycLimitsItem.FeatureWithLimitItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        FeatureWithLimitItemViewHolder(
            ItemFeatureLimitsFeatureBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(items: List<KycLimitsItem>, position: Int, holder: RecyclerView.ViewHolder) =
        (holder as FeatureWithLimitItemViewHolder).bind(items[position] as KycLimitsItem.FeatureWithLimitItem)
}

class FeatureWithLimitItemViewHolder(
    val binding: ItemFeatureLimitsFeatureBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: KycLimitsItem.FeatureWithLimitItem) {
        with(binding) {
            iconFeature.setImageResource(
                when (item.feature) {
                    Feature.SEND_FROM_TRADING_TO_PRIVATE -> R.drawable.ic_icon_send
                    Feature.RECEIVE_TO_TRADING -> R.drawable.ic_qr_scan
                    Feature.SWAP -> R.drawable.ic_vector_toolbar_swap
                    Feature.BUY_SELL -> R.drawable.ic_fiat_notes
                    Feature.CARD_PURCHASES -> R.drawable.vector_card
                    Feature.FIAT_DEPOSIT -> R.drawable.ic_transfer_bank
                    Feature.FIAT_WITHDRAWAL -> R.drawable.ic_transfer_bank
                    Feature.REWARDS -> R.drawable.ic_bottom_nav_rewards
                }
            )

            textFeature.text = when (item.feature) {
                Feature.SEND_FROM_TRADING_TO_PRIVATE -> context.getString(R.string.feature_limits_send_crypto)
                Feature.RECEIVE_TO_TRADING -> context.getString(R.string.feature_limits_receive_crypto)
                Feature.SWAP -> context.getString(R.string.feature_limits_swap_crypto)
                Feature.BUY_SELL -> context.getString(R.string.feature_limits_buy_sell)
                Feature.CARD_PURCHASES -> context.getString(R.string.feature_limits_card_purchases)
                Feature.FIAT_DEPOSIT -> context.getString(R.string.feature_limits_deposits)
                Feature.FIAT_WITHDRAWAL -> context.getString(R.string.feature_limits_withdrawals)
                Feature.REWARDS -> context.getString(R.string.feature_limits_earn_rewards)
            }
            val featureDescriptionText = when (item.feature) {
                Feature.SEND_FROM_TRADING_TO_PRIVATE -> context.getString(R.string.feature_limits_from_trading_accounts)
                Feature.RECEIVE_TO_TRADING -> context.getString(R.string.feature_limits_to_trading_accounts)
                Feature.SWAP -> context.getString(R.string.feature_limits_swap_description)
                Feature.BUY_SELL -> context.getString(R.string.feature_limits_buy_sell_description)
                Feature.CARD_PURCHASES -> context.getString(R.string.feature_limits_card_purchases_description)
                Feature.FIAT_DEPOSIT -> null
                Feature.FIAT_WITHDRAWAL -> context.getString(R.string.feature_limits_withdrawals_description)
                Feature.REWARDS -> context.getString(R.string.feature_limits_earn_rewards_description)
            }
            textFeatureDescription.text = featureDescriptionText
            textFeatureDescription.visibleIf { featureDescriptionText != null }

            textLimit.text = when (val featureLimit = item.limit) {
                FeatureLimit.Disabled -> context.getString(R.string.feature_limits_disabled)
                FeatureLimit.Infinite -> context.getString(R.string.feature_limits_no_limit)
                is FeatureLimit.Limited -> featureLimit.limit.amount.formatOrSymbolForZero()
                FeatureLimit.Unspecified -> context.getString(R.string.feature_limits_enabled)
            }

            val featureLimit = item.limit
            val limitPeriodDescription = if (featureLimit is FeatureLimit.Limited) {
                when (featureLimit.limit.period) {
                    TxLimitPeriod.DAILY -> context.getString(R.string.feature_limits_daily_limit_period)
                    TxLimitPeriod.MONTHLY -> context.getString(R.string.feature_limits_monthly_limit_period)
                    TxLimitPeriod.YEARLY -> context.getString(R.string.feature_limits_yearly_limit_period)
                }
            } else {
                null
            }
            textLimitPeriod.text = limitPeriodDescription
            textLimitPeriod.visibleIf { featureDescriptionText != null }
        }
    }
}

class FooterItemDelegate : AdapterDelegate<KycLimitsItem> {
    override fun isForViewType(items: List<KycLimitsItem>, position: Int): Boolean =
        items[position] is KycLimitsItem.FooterItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        FooterItemViewHolder(
            ItemFeatureLimitsFooterBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(items: List<KycLimitsItem>, position: Int, holder: RecyclerView.ViewHolder) =
        (holder as FooterItemViewHolder).bind()
}

private class FooterItemViewHolder(
    val binding: ItemFeatureLimitsFooterBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind() {
        with(binding) {
            val links = mapOf("support_center_link" to Uri.parse(LIMITS_SUPPORT_CENTER))
            val parsedString =
                StringUtils.getStringWithMappedAnnotations(context, R.string.feature_limits_footer, links)

            textFooter.text = parsedString
            textFooter.movementMethod = LinkMovementMethod.getInstance()
        }
    }
}
