package com.blockchain.earn.dashboard

import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Lock
import com.blockchain.componentlib.icons.Prices
import com.blockchain.componentlib.icons.Rewards
import com.blockchain.componentlib.utils.TextValue
import com.blockchain.stringResources.R

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
    data class PassiveRewardsUiElement(val rate: Double) : EarnProductUiElement(
        header = EarnProductPropertyRow(
            primaryText = TextValue.IntResValue(R.string.earn_rewards_label_passive),
            secondaryText = TextValue.IntResValue(R.string.earn_passive_rewards_description),
            imageResource = Icons.Filled.Rewards
        ),

        targetAudience = EarnProductPropertyRow(
            primaryText = TextValue.IntResValue(R.string.earn_product_target_audience_all),
            imageResource = targetAudienceIcon
        ),

        availableAssets = EarnProductPropertyRow(
            primaryText = TextValue.IntResValue(R.string.earn_product_asset_availability_all),
            imageResource = availableAssetsIcon
        ),

        earnRate = EarnProductPropertyRow(
            primaryText = TextValue.IntResValue(R.string.earn_passive_rate_label, args = listOf(rate)),
            secondaryText = TextValue.IntResValue(R.string.earn_rate_update_frequency_monthly),
            imageResource = earnRateIcon
        ),

        earnFrequency = EarnProductPropertyRow(
            primaryText = TextValue.IntResValue(R.string.earn_product_earn_frequency_daily),
            imageResource = earnFrequencyIcon
        ),

        payoutFrequency = EarnProductPropertyRow(
            primaryText = TextValue.IntResValue(R.string.earn_product_payout_frequency_monthly),
            imageResource = payoutFrequencyIcon
        ),

        withdrawalFrequency = EarnProductPropertyRow(
            primaryText = TextValue.IntResValue(R.string.earn_product_withdrawal_frequency_instantly),
            imageResource = withdrawalFrequencyIcon
        )
    )

    data class StakingRewardsUiElement(val rate: Double) : EarnProductUiElement(
        header = EarnProductPropertyRow(
            primaryText = TextValue.IntResValue(R.string.earn_rewards_label_staking),
            secondaryText = TextValue.IntResValue(R.string.earn_staking_rewards_description),
            imageResource = Icons.Filled.Lock
        ),

        targetAudience = EarnProductPropertyRow(
            primaryText = TextValue.IntResValue(R.string.earn_product_target_audience_intermediate),
            imageResource = targetAudienceIcon
        ),

        availableAssets = EarnProductPropertyRow(
            primaryText = TextValue.IntResValue(R.string.earn_product_asset_availability_ethereum),
            imageResource = availableAssetsIcon
        ),

        earnRate = EarnProductPropertyRow(
            primaryText = TextValue.IntResValue(R.string.earn_staking_rate_label, args = listOf(rate)),
            secondaryText = TextValue.IntResValue(R.string.earn_rate_update_frequency_variable),
            imageResource = earnRateIcon
        ),

        earnFrequency = EarnProductPropertyRow(
            primaryText = TextValue.IntResValue(R.string.earn_product_earn_frequency_daily),
            imageResource = earnFrequencyIcon
        ),

        payoutFrequency = EarnProductPropertyRow(
            primaryText = TextValue.IntResValue(R.string.earn_product_payout_frequency_daily),
            imageResource = payoutFrequencyIcon
        ),

        withdrawalFrequency = EarnProductPropertyRow(
            primaryText = TextValue.IntResValue(R.string.earn_product_withdrawal_frequency_variable),
            imageResource = withdrawalFrequencyIcon
        )
    )

    data class ActiveRewardsUiElement(val rate: Double) : EarnProductUiElement(
        header = EarnProductPropertyRow(
            primaryText = TextValue.IntResValue(R.string.earn_rewards_label_active),
            secondaryText = TextValue.IntResValue(R.string.earn_active_rewards_description),
            imageResource = Icons.Filled.Prices
        ),

        targetAudience = EarnProductPropertyRow(
            primaryText = TextValue.IntResValue(R.string.earn_product_target_audience_advanced),
            imageResource = targetAudienceIcon
        ),

        availableAssets = EarnProductPropertyRow(
            primaryText = TextValue.IntResValue(R.string.earn_product_asset_availability_bitcoin),
            imageResource = availableAssetsIcon
        ),

        earnRate = EarnProductPropertyRow(
            primaryText = TextValue.IntResValue(R.string.earn_active_rate_label, args = listOf(rate)),
            secondaryText = TextValue.IntResValue(R.string.earn_rate_update_frequency_variable),
            imageResource = earnRateIcon
        ),

        earnFrequency = EarnProductPropertyRow(
            primaryText = TextValue.IntResValue(R.string.earn_product_earn_frequency_weekly),
            imageResource = earnFrequencyIcon
        ),

        payoutFrequency = EarnProductPropertyRow(
            primaryText = TextValue.IntResValue(R.string.earn_product_payout_frequency_weekly),
            imageResource = payoutFrequencyIcon
        ),

        withdrawalFrequency = EarnProductPropertyRow(
            primaryText = TextValue.IntResValue(R.string.earn_product_withdrawal_frequency_weekly),
            imageResource = withdrawalFrequencyIcon
        )
    )
}

data class EarnProductPropertyRow(
    val primaryText: TextValue,
    val secondaryText: TextValue? = null,
    val imageResource: ImageResource.Local
)
