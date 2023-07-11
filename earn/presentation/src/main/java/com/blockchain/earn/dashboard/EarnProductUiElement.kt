package com.blockchain.earn.dashboard

import androidx.annotation.StringRes
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Lock
import com.blockchain.componentlib.icons.Prices
import com.blockchain.componentlib.icons.Rewards

val targetAudienceIcon = ImageResource.Local(
    id = com.blockchain.componentlib.icons.R.drawable.users_off,
)

val availableAssetsIcon = ImageResource.Local(
    id = com.blockchain.componentlib.icons.R.drawable.coins_off,
)

val earnRateIcon = ImageResource.Local(
    id = com.blockchain.componentlib.icons.R.drawable.rewards_off,
)

val earnFrequencyIcon = ImageResource.Local(
    id = com.blockchain.componentlib.icons.R.drawable.usd_off,
)

val payoutFrequencyIcon = ImageResource.Local(
    id = com.blockchain.componentlib.icons.R.drawable.wallet_off,
)

val withdrawalFrequencyIcon = ImageResource.Local(
    id = com.blockchain.componentlib.icons.R.drawable.send_off,
)

sealed class EarnProductUiElement(
    val header: EarnProductPropertyRow,
    val targetAudience: EarnProductPropertyRow,
    val availableAssets: EarnProductPropertyRow,
    val earnRate: EarnProductPropertyRow,
    val earnFrequency: EarnProductPropertyRow,
    val payoutFrequency: EarnProductPropertyRow,
    val withdrawalFrequency: EarnProductPropertyRow
) {
    object PassiveRewardsUiElement : EarnProductUiElement(
        header = EarnProductPropertyRow(
            primaryTextId = com.blockchain.stringResources.R.string.earn_rewards_label_passive,
            secondaryTextId = com.blockchain.stringResources.R.string.earn_passive_rewards_description,
            imageResource = Icons.Filled.Rewards
        ),

        targetAudience = EarnProductPropertyRow(
            primaryTextId = com.blockchain.stringResources.R.string.earn_product_target_audience_all,
            imageResource = targetAudienceIcon
        ),

        availableAssets = EarnProductPropertyRow(
            primaryTextId = com.blockchain.stringResources.R.string.earn_product_asset_availability_all,
            imageResource = availableAssetsIcon
        ),

        earnRate = EarnProductPropertyRow(
            primaryTextId = com.blockchain.stringResources.R.string.earn_passive_rate_label,
            secondaryTextId = com.blockchain.stringResources.R.string.earn_rate_update_frequency_monthly,
            imageResource = earnRateIcon
        ),

        earnFrequency = EarnProductPropertyRow(
            primaryTextId = com.blockchain.stringResources.R.string.earn_product_earn_frequency_daily,
            imageResource = earnFrequencyIcon
        ),

        payoutFrequency = EarnProductPropertyRow(
            primaryTextId = com.blockchain.stringResources.R.string.earn_product_payout_frequency_monthly,
            imageResource = payoutFrequencyIcon
        ),

        withdrawalFrequency = EarnProductPropertyRow(
            primaryTextId = com.blockchain.stringResources.R.string.earn_product_withdrawal_frequency_instantly,
            imageResource = withdrawalFrequencyIcon
        )
    )

    object StakingRewardsUiElement : EarnProductUiElement(
        header = EarnProductPropertyRow(
            primaryTextId = com.blockchain.stringResources.R.string.earn_rewards_label_staking,
            secondaryTextId = com.blockchain.stringResources.R.string.earn_staking_rewards_description,
            imageResource = Icons.Filled.Lock
        ),

        targetAudience = EarnProductPropertyRow(
            primaryTextId = com.blockchain.stringResources.R.string.earn_product_target_audience_intermediate,
            imageResource = targetAudienceIcon
        ),

        availableAssets = EarnProductPropertyRow(
            primaryTextId = com.blockchain.stringResources.R.string.earn_product_asset_availability_ethereum,
            imageResource = availableAssetsIcon
        ),

        earnRate = EarnProductPropertyRow(
            primaryTextId = com.blockchain.stringResources.R.string.earn_staking_rate_label,
            secondaryTextId = com.blockchain.stringResources.R.string.earn_rate_update_frequency_variable,
            imageResource = earnRateIcon
        ),

        earnFrequency = EarnProductPropertyRow(
            primaryTextId = com.blockchain.stringResources.R.string.earn_product_earn_frequency_daily,
            imageResource = earnFrequencyIcon
        ),

        payoutFrequency = EarnProductPropertyRow(
            primaryTextId = com.blockchain.stringResources.R.string.earn_product_payout_frequency_daily,
            imageResource = payoutFrequencyIcon
        ),

        withdrawalFrequency = EarnProductPropertyRow(
            primaryTextId = com.blockchain.stringResources.R.string.earn_product_withdrawal_frequency_variable,
            imageResource = withdrawalFrequencyIcon
        )
    )

    object ActiveRewardsUiElement : EarnProductUiElement(
        header = EarnProductPropertyRow(
            primaryTextId = com.blockchain.stringResources.R.string.earn_rewards_label_active,
            secondaryTextId = com.blockchain.stringResources.R.string.earn_active_rewards_description,
            imageResource = Icons.Filled.Prices
        ),

        targetAudience = EarnProductPropertyRow(
            primaryTextId = com.blockchain.stringResources.R.string.earn_product_target_audience_advanced,
            imageResource = targetAudienceIcon
        ),

        availableAssets = EarnProductPropertyRow(
            primaryTextId = com.blockchain.stringResources.R.string.earn_product_asset_availability_bitcoin,
            imageResource = availableAssetsIcon
        ),

        earnRate = EarnProductPropertyRow(
            primaryTextId = com.blockchain.stringResources.R.string.earn_active_rate_label,
            secondaryTextId = com.blockchain.stringResources.R.string.earn_rate_update_frequency_variable,
            imageResource = earnRateIcon
        ),

        earnFrequency = EarnProductPropertyRow(
            primaryTextId = com.blockchain.stringResources.R.string.earn_product_earn_frequency_weekly,
            imageResource = earnFrequencyIcon
        ),

        payoutFrequency = EarnProductPropertyRow(
            primaryTextId = com.blockchain.stringResources.R.string.earn_product_payout_frequency_weekly,
            imageResource = payoutFrequencyIcon
        ),

        withdrawalFrequency = EarnProductPropertyRow(
            primaryTextId = com.blockchain.stringResources.R.string.earn_product_withdrawal_frequency_weekly,
            imageResource = withdrawalFrequencyIcon
        )
    )
}

data class EarnProductPropertyRow(
    @StringRes val primaryTextId: Int,
    @StringRes val secondaryTextId: Int? = null,
    val imageResource: ImageResource.Local
)
