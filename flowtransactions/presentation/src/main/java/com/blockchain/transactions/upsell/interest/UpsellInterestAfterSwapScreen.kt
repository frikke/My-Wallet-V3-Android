package com.blockchain.transactions.upsell.interest

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.analytics.Analytics
import com.blockchain.betternavigation.utils.Bindable
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.impl.CustodialInterestAccount
import com.blockchain.componentlib.basic.CloseIcon
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.MinimalPrimaryButton
import com.blockchain.componentlib.button.MinimalPrimarySmallButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.icon.SmallTagIcon
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Rewards
import com.blockchain.componentlib.sheets.SheetFlatHeader
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.EpicVerticalSpacer
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.theme.StandardVerticalSpacer
import com.blockchain.componentlib.utils.openUrl
import com.blockchain.koin.payloadScope
import com.blockchain.presentation.urllinks.EARN_LEARN_MORE_URL
import com.blockchain.stringResources.R
import com.blockchain.transactions.swap.SwapService
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import org.koin.androidx.compose.get
import java.io.Serializable
import java.text.DecimalFormat

data class UpsellInterestArgs(
    val sourceAccount: Bindable<CryptoAccount>,
    val targetAccount: Bindable<CustodialInterestAccount>,
    val interestRate: Double,
) : Serializable

@Composable
fun UpsellInterestAfterSwapScreen(
    analytics: Analytics = get(),
    args: UpsellInterestArgs,
    swapService: SwapService = get(scope = payloadScope),
    navigateToInterestDeposit: (source: CryptoAccount, target: CustodialInterestAccount) -> Unit,
    exitFlow: () -> Unit,
) {
    val sourceAccount = args.sourceAccount.data ?: return
    val targetAccount = args.targetAccount.data ?: return
    val interestRate = args.interestRate
    val assetToUpsell = targetAccount.currency

    LaunchedEffect(Unit) {
        analytics.logEvent(UpsellInterestAfterSwapViewed(assetToUpsell.networkTicker))
    }

    val context = LocalContext.current

    Content(
        assetToUpsell = assetToUpsell,
        interestRate = interestRate,
        onLearnMore = {
            context.openUrl(EARN_LEARN_MORE_URL)
        },
        onStartEarning = {
            analytics.logEvent(UpsellInterestAfterSwapStartEarningClicked(assetToUpsell.networkTicker))
            navigateToInterestDeposit(sourceAccount, targetAccount)
        },
        onMaybeLater = {
            analytics.logEvent(UpsellInterestAfterSwapMaybeLaterClicked(assetToUpsell.networkTicker))
            swapService.dismissUpsellPassiveRewardsAfterSwap()
            exitFlow()
        },
        onCloseClick = {
            analytics.logEvent(UpsellInterestAfterSwapDismissed(assetToUpsell.networkTicker))
            exitFlow()
        }
    )
}

@Composable
private fun Content(
    assetToUpsell: AssetInfo,
    interestRate: Double,
    onLearnMore: () -> Unit,
    onStartEarning: () -> Unit,
    onMaybeLater: () -> Unit,
    onCloseClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .background(color = AppColors.background)
            .fillMaxWidth()
            .padding(horizontal = AppTheme.dimensions.smallSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppTheme.dimensions.smallSpacing),
        ) {
            CloseIcon(
                modifier = Modifier.align(Alignment.CenterEnd),
                isScreenBackgroundSecondary = false,
                onClick = onCloseClick
            )
        }

        EpicVerticalSpacer()

        val assetIcon = ImageResource.Remote(
            url = assetToUpsell.logo,
            shape = CircleShape,
            size = 88.dp
        )
        val tagIcon = Icons.Filled.Rewards
            .withTint(AppTheme.colors.primary)
            .withSize(44.dp)

        val stackedIcon = StackedIcon.SmallTag(
            main = assetIcon,
            tag = tagIcon
        )

        SmallTagIcon(
            icon = stackedIcon,
            iconBackground = AppTheme.colors.light,
            mainIconSize = 88.dp,
            tagIconSize = 44.dp,
            borderColor = AppTheme.colors.light
        )

        SmallVerticalSpacer()

        SimpleText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.dimensions.standardSpacing),
            text = stringResource(R.string.interest_upsell_after_swap_title, assetToUpsell.name),
            style = ComposeTypographies.Title3,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Centre
        )

        SmallVerticalSpacer()

        SimpleText(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.dimensions.standardSpacing),
            text = stringResource(
                R.string.interest_upsell_after_swap_subtitle,
                DecimalFormat("0.#").format(interestRate),
                assetToUpsell.name
            ),
            style = ComposeTypographies.Body1,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Centre
        )

        StandardVerticalSpacer()

        MinimalPrimarySmallButton(
            text = stringResource(R.string.common_learn_more),
            onClick = onLearnMore
        )

        Spacer(Modifier.weight(1f))

        PrimaryButton(
            text = stringResource(R.string.interest_upsell_after_swap_cta),
            onClick = onStartEarning,
            modifier = Modifier.fillMaxWidth(),
        )

        SmallVerticalSpacer()

        MinimalPrimaryButton(
            text = stringResource(R.string.common_maybe_later),
            onClick = onMaybeLater,
            modifier = Modifier.fillMaxWidth()
        )

        StandardVerticalSpacer()
    }
}

@Preview
@Composable
private fun UpSellAnotherAssetScreenPreview() {
    AppTheme {
        Content(
            assetToUpsell = CryptoCurrency.BTC,
            interestRate = 4.5,
            onLearnMore = {},
            onStartEarning = {},
            onMaybeLater = {},
            onCloseClick = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun UpSellAnotherAssetScreenPreviewDark() {
    UpSellAnotherAssetScreenPreview()
}

