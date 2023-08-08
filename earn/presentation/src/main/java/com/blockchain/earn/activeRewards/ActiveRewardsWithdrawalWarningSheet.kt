package com.blockchain.earn.activeRewards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.commonarch.presentation.base.ComposeModalBottomDialog
import com.blockchain.commonarch.presentation.base.HostedBottomSheet
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.MinimalPrimarySmallButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.theme.StandardVerticalSpacer
import com.blockchain.componentlib.theme.TinyVerticalSpacer
import com.blockchain.earn.R
import org.koin.androidx.compose.get

const val ACTIVE_REWARDS_LEARN_MORE_URL =
    "https://support.blockchain.com/hc/en-us/articles/6868491485724-What-is-Active-Rewards-"
class ActiveRewardsWithdrawalWarningSheet : ComposeModalBottomDialog() {

    override val host: Host by lazy {
        activity as? Host ?: parentFragment as? Host ?: throw IllegalStateException(
            "Host is not a ActiveRewardsWithdrawalWarningSheet.Host"
        )
    }

    override val makeSheetNonCollapsible: Boolean
        get() = true

    interface Host : HostedBottomSheet.Host {
        fun openExternalUrl(url: String)
        fun onNextClicked()
        fun onClose()
    }

    @Composable
    override fun Sheet() {
        ActiveRewardsWithdrawalWarning(
            dismiss = ::dismiss,
            onClose = { host.onClose() },
            onLearnMoreClicked = { host.openExternalUrl(ACTIVE_REWARDS_LEARN_MORE_URL) },
            onWithdrawDisabledLearnMoreClicked = { host.openExternalUrl(WITHDRAWALS_DISABLED_LEARN_MORE_URL) },
            onNext = host::onNextClicked
        )
    }

    companion object {
        fun newInstance() =
            ActiveRewardsWithdrawalWarningSheet()
    }
}

@Composable
fun ActiveRewardsWithdrawalWarning(
    dismiss: () -> Unit,
    onClose: () -> Unit,
    onLearnMoreClicked: () -> Unit,
    onWithdrawDisabledLearnMoreClicked: () -> Unit,
    onNext: () -> Unit,
) {
    var withdrawalsEnabled = true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background, RoundedCornerShape(AppTheme.dimensions.tinySpacing))
            .padding(horizontal = AppTheme.dimensions.standardSpacing)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            SheetHeader(
                title = "",
                onClosePress = {
                    onClose()
                    dismiss()
                },
            )

            TinyVerticalSpacer()

            Image(
                imageResource = ImageResource.Local(
                    R.drawable.ic_active_rewards_account_indicator,
                    colorFilter = ColorFilter.tint(Color.White),
                    shape = CircleShape,
                    size = 88.dp
                ),
                modifier = Modifier
                    .background(AppTheme.colors.title, CircleShape)
            )

            StandardVerticalSpacer()

            SimpleText(
                text = stringResource(
                    id = com.blockchain.stringResources.R.string.earn_active_rewards_withdrawal_blocked_warning_title
                ),
                style = ComposeTypographies.Title3,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Centre
            )

            SmallVerticalSpacer()

            SimpleText(
                text = stringResource(
                    id = com.blockchain.stringResources.R.string.earn_active_rewards_withdrawal_blocked_warning_subtitle
                ),
                style = ComposeTypographies.Body1,
                color = ComposeColors.Body,
                gravity = ComposeGravities.Centre
            )

            StandardVerticalSpacer()

            MinimalPrimarySmallButton(
                text = stringResource(com.blockchain.stringResources.R.string.common_learn_more),
                onClick = onLearnMoreClicked
            )

            StandardVerticalSpacer()

            if (withdrawalsEnabled.not()) {
                ActiveRewardsWithdrawalNotice(onWithdrawDisabledLearnMoreClicked)
            }
        }

        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppTheme.dimensions.standardSpacing),
            text = stringResource(id = com.blockchain.stringResources.R.string.common_next),
            onClick = {
                onNext()
                dismiss()
            }
        )
    }
}

@Preview()
@Composable
fun PreviewActiveRewardsWithdrawalWarning() {
    AppTheme {
        AppSurface {
            ActiveRewardsWithdrawalWarning(
                {},
                {},
                {},
                {},
                {}
            )
        }
    }
}
