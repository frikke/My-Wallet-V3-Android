package com.blockchain.earn.dashboard

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.BackgroundMuted
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.componentlib.theme.Grey900

val targetAudienceIcon = ImageResource.LocalWithBackground(
    id = R.drawable.users_off,
    iconColor = Grey900,
    backgroundColor = Color.White,
    contentDescription = null
)

val availableAssetsIcon = ImageResource.LocalWithBackground(
    id = R.drawable.coins_off,
    iconColor = Grey900,
    backgroundColor = Color.White,
    contentDescription = null
)

val earnRateIcon = ImageResource.LocalWithBackground(
    id = R.drawable.rewards_off,
    iconColor = Grey900,
    backgroundColor = Color.White,
    contentDescription = null
)

val earnFrequencyIcon = ImageResource.LocalWithBackground(
    id = R.drawable.usd_off,
    iconColor = Grey900,
    backgroundColor = Color.White,
    contentDescription = null
)

val payoutFrequencyIcon = ImageResource.LocalWithBackground(
    id = R.drawable.wallet_off,
    iconColor = Grey900,
    backgroundColor = Color.White,
    contentDescription = null
)

val withdrawalFrequencyIcon = ImageResource.LocalWithBackground(
    id = R.drawable.send_off,
    iconColor = Grey900,
    backgroundColor = Color.White,
    contentDescription = null
)

sealed class EarnProductUiElement(
    val header: EarnProductPropertyRow,
    val targetAudience: EarnProductPropertyRow,
    val availableAssets: EarnProductPropertyRow,
    val earnRate: EarnProductPropertyRow,
    val earnFrequency: EarnProductPropertyRow,
    val payoutFrequency: EarnProductPropertyRow,
    val withdrawalFrequency: EarnProductPropertyRow,
) {
    object PassiveRewardsUiElement : EarnProductUiElement(
        header = EarnProductPropertyRow(
            primaryTextId = R.string.earn_rewards_label_passive,
            secondaryTextId = R.string.earn_passive_rewards_description,
            imageResource = ImageResource.LocalWithBackground(
                id = R.drawable.rewards_on,
                iconColor = Blue600,
                backgroundColor = BackgroundMuted,
                contentDescription = null
            )
        ),

        targetAudience = EarnProductPropertyRow(
            primaryTextId = R.string.earn_product_target_audience_all,
            imageResource = targetAudienceIcon
        ),

        availableAssets = EarnProductPropertyRow(
            primaryTextId = R.string.earn_product_asset_availability_all,
            imageResource = availableAssetsIcon
        ),

        earnRate = EarnProductPropertyRow(
            primaryTextId = R.string.earn_passive_rate_label,
            secondaryTextId = R.string.earn_rate_update_frequency_monthly,
            imageResource = earnRateIcon
        ),

        earnFrequency = EarnProductPropertyRow(
            primaryTextId = R.string.earn_product_earn_frequency_daily,
            imageResource = earnFrequencyIcon
        ),

        payoutFrequency = EarnProductPropertyRow(
            primaryTextId = R.string.earn_product_payout_frequency_monthly,
            imageResource = payoutFrequencyIcon
        ),

        withdrawalFrequency = EarnProductPropertyRow(
            primaryTextId = R.string.earn_product_withdrawal_frequency_instantly,
            imageResource = withdrawalFrequencyIcon
        )
    )

    object StakingRewardsUiElement : EarnProductUiElement(
        header = EarnProductPropertyRow(
            primaryTextId = R.string.earn_rewards_label_staking,
            secondaryTextId = R.string.earn_staking_rewards_description,
            imageResource = ImageResource.LocalWithBackground(
                id = R.drawable.lock_on,
                iconColor = Blue600,
                backgroundColor = BackgroundMuted,
                contentDescription = null
            )
        ),

        targetAudience = EarnProductPropertyRow(
            primaryTextId = R.string.earn_product_target_audience_intermediate,
            imageResource = targetAudienceIcon
        ),

        availableAssets = EarnProductPropertyRow(
            primaryTextId = R.string.earn_product_asset_availability_ethereum,
            imageResource = availableAssetsIcon
        ),

        earnRate = EarnProductPropertyRow(
            primaryTextId = R.string.earn_staking_rate_label,
            secondaryTextId = R.string.earn_rate_update_frequency_variable,
            imageResource = earnRateIcon
        ),

        earnFrequency = EarnProductPropertyRow(
            primaryTextId = R.string.earn_product_earn_frequency_daily,
            imageResource = earnFrequencyIcon
        ),

        payoutFrequency = EarnProductPropertyRow(
            primaryTextId = R.string.earn_product_payout_frequency_daily,
            imageResource = payoutFrequencyIcon
        ),

        withdrawalFrequency = EarnProductPropertyRow(
            primaryTextId = R.string.earn_product_withdrawal_frequency_variable,
            imageResource = withdrawalFrequencyIcon
        )
    )

    object ActiveRewardsUiElement : EarnProductUiElement(
        header = EarnProductPropertyRow(
            primaryTextId = R.string.earn_rewards_label_active,
            secondaryTextId = R.string.earn_active_rewards_description,
            imageResource = ImageResource.LocalWithBackground(
                id = R.drawable.prices_on,
                iconColor = Blue600,
                backgroundColor = BackgroundMuted,
                contentDescription = null
            )
        ),

        targetAudience = EarnProductPropertyRow(
            primaryTextId = R.string.earn_product_target_audience_advanced,
            imageResource = targetAudienceIcon
        ),

        availableAssets = EarnProductPropertyRow(
            primaryTextId = R.string.earn_product_asset_availability_bitcoin,
            imageResource = availableAssetsIcon
        ),

        earnRate = EarnProductPropertyRow(
            primaryTextId = R.string.earn_active_rate_label,
            secondaryTextId = R.string.earn_rate_update_frequency_variable,
            imageResource = earnRateIcon
        ),

        earnFrequency = EarnProductPropertyRow(
            primaryTextId = R.string.earn_product_earn_frequency_weekly,
            imageResource = earnFrequencyIcon
        ),

        payoutFrequency = EarnProductPropertyRow(
            primaryTextId = R.string.earn_product_payout_frequency_weekly,
            imageResource = payoutFrequencyIcon
        ),

        withdrawalFrequency = EarnProductPropertyRow(
            primaryTextId = R.string.earn_product_withdrawal_frequency_weekly,
            imageResource = withdrawalFrequencyIcon
        )
    )
}

data class EarnProductPropertyRow(
    @StringRes val primaryTextId: Int,
    @StringRes val secondaryTextId: Int? = null,
    val imageResource: ImageResource.LocalWithBackground
)
