package com.blockchain.earn.activeRewards

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.EarnRewardsAccount
import com.blockchain.coincore.impl.CustodialTradingAccount
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.SecondaryButton
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.tablerow.custom.TextWithTooltipTableRow
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.LargeVerticalSpacer
import com.blockchain.componentlib.theme.TinyHorizontalSpacer
import com.blockchain.componentlib.theme.TinyVerticalSpacer
import com.blockchain.componentlib.theme.topOnly
import com.blockchain.earn.activeRewards.viewmodel.ActiveRewardsError
import com.blockchain.earn.activeRewards.viewmodel.ActiveRewardsSummaryViewState
import com.blockchain.earn.common.EarnFieldExplainer
import com.blockchain.earn.common.EarnPendingWithdrawalFullBalance
import com.blockchain.earn.domain.models.EarnRewardsFrequency
import com.blockchain.extensions.safeLet
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.koin.activeRewardsWithdrawalsFeatureFlag
import org.koin.androidx.compose.get

@Composable
fun ActiveRewardsSummarySheet(
    state: ActiveRewardsSummaryViewState,
    onWithdrawPressed: (sourceAccount: BlockchainAccount, targetAccount: CustodialTradingAccount) -> Unit,
    onDepositPressed: (currency: EarnRewardsAccount.Active) -> Unit,
    withdrawDisabledLearnMore: () -> Unit,
    onExplainerClicked: (EarnFieldExplainer) -> Unit,
    onClosePressed: () -> Unit
) {
    Surface(
        color = AppColors.background,
        shape = AppTheme.shapes.large.topOnly()
    ) {
        Column {
            SheetHeader(
                title = stringResource(
                    id = com.blockchain.stringResources.R.string.active_rewards_summary_title,
                    state.balanceCrypto?.currency?.networkTicker.orEmpty()
                ),
                startImage = StackedIcon.SingleIcon(
                    ImageResource.Remote(state.balanceCrypto?.currency?.logo.orEmpty())
                ),
                onClosePress = onClosePressed,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F)
                    .padding(horizontal = AppTheme.dimensions.standardSpacing)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LargeVerticalSpacer()

                state.balanceFiat?.let { balance ->
                    SimpleText(
                        text = balance.toStringWithSymbol(),
                        style = ComposeTypographies.Title1,
                        color = ComposeColors.Title,
                        gravity = ComposeGravities.Centre
                    )
                    TinyVerticalSpacer()
                }

                state.balanceCrypto?.let { balance ->
                    SimpleText(
                        text = balance.toStringWithSymbol(),
                        style = ComposeTypographies.Body2,
                        color = ComposeColors.Body,
                        gravity = ComposeGravities.Centre
                    )
                }

                LargeVerticalSpacer()

                Card(
                    backgroundColor = AppTheme.colors.backgroundSecondary,
                    shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
                    elevation = 0.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TinyVerticalSpacer()

                        safeLet(
                            state.balanceCrypto?.currency?.networkTicker,
                            state.assetFiatPrice
                        ) { currencyTicker, assetFiatPrice ->
                            TextWithTooltipTableRow(
                                startText = stringResource(
                                    id = com.blockchain.stringResources.R.string.quote_price,
                                    currencyTicker
                                ),
                                endTitle = assetFiatPrice.toStringWithSymbol()
                            )
                        }

                        if (state.totalEarnedFiat != null && state.totalEarnedCrypto != null) {
                            TextWithTooltipTableRow(
                                startText = stringResource(
                                    com.blockchain.stringResources.R.string.earn_active_rewards_earnings
                                ),
                                endTitle = state.totalEarnedFiat.toStringWithSymbol(),
                                endSubtitle = state.totalEarnedCrypto.toStringWithSymbol(),
                                onClick = {
                                    onExplainerClicked(EarnFieldExplainer.ActiveRewardsEarnings)
                                }
                            )
                        }

                        if (state.totalSubscribedFiat != null && state.totalSubscribedCrypto != null) {
                            TextWithTooltipTableRow(
                                startText = stringResource(
                                    com.blockchain.stringResources.R.string.earn_total_subscribed
                                ),
                                endTitle = state.totalSubscribedFiat.toStringWithSymbol(),
                                endSubtitle = state.totalSubscribedCrypto.toStringWithSymbol()
                            )
                        }

                        if (state.totalOnHoldFiat != null && state.totalOnHoldCrypto != null) {
                            TextWithTooltipTableRow(
                                startText = stringResource(
                                    com.blockchain.stringResources.R.string.earn_active_rewards_on_hold
                                ),
                                endTitle = state.totalOnHoldFiat.toStringWithSymbol(),
                                endSubtitle = state.totalOnHoldCrypto.toStringWithSymbol(),
                                onClick = {
                                    onExplainerClicked(EarnFieldExplainer.ActiveRewardsOnHold)
                                }
                            )
                        }
                        TinyVerticalSpacer()
                    }
                }

                LargeVerticalSpacer()

                Card(
                    backgroundColor = AppTheme.colors.backgroundSecondary,
                    shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
                    elevation = 0.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TinyVerticalSpacer()

                        TextWithTooltipTableRow(
                            startText = stringResource(com.blockchain.stringResources.R.string.earn_annual_rate),
                            endTitle = "${state.activeRewardsRate}%",
                            onClick = {
                                onExplainerClicked(EarnFieldExplainer.ActiveRewardsEarnRate)
                            }
                        )

                        if (state.triggerPrice != null) {
                            TextWithTooltipTableRow(
                                startText = stringResource(
                                    com.blockchain.stringResources.R.string.earn_active_rewards_trigger_price
                                ),
                                endTitle = state.triggerPrice.toStringWithSymbol(),
                                onClick = {
                                    onExplainerClicked(EarnFieldExplainer.ActiveRewardsTriggerPrice)
                                }
                            )
                        }

                        TextWithTooltipTableRow(
                            startText = stringResource(com.blockchain.stringResources.R.string.earn_payment_frequency),
                            endTitle = when (state.rewardsFrequency) {
                                EarnRewardsFrequency.Daily ->
                                    stringResource(
                                        id = com.blockchain.stringResources.R.string.earn_payment_frequency_daily
                                    )

                                EarnRewardsFrequency.Weekly ->
                                    stringResource(
                                        id = com.blockchain.stringResources.R.string.earn_payment_frequency_weekly
                                    )

                                EarnRewardsFrequency.Monthly ->
                                    stringResource(
                                        id = com.blockchain.stringResources.R.string.earn_payment_frequency_monthly
                                    )

                                else ->
                                    stringResource(
                                        id = com.blockchain.stringResources.R.string.earn_payment_frequency_unknown
                                    )
                            }
                        )

                        TinyVerticalSpacer()
                    }
                }

                LargeVerticalSpacer()

                // TODO(labreu): no point in adding doing this check in the viewmodel since this will all be removed very soon once withdrawals are enabled in prod
                var activeRewardsWithdrawalsEnabled by remember { mutableStateOf(false) }
                val activeRewardsWithdrawalsFF = get<FeatureFlag>(activeRewardsWithdrawalsFeatureFlag)
                LaunchedEffect(activeRewardsWithdrawalsFF) {
                    activeRewardsWithdrawalsEnabled = activeRewardsWithdrawalsFF.coEnabled()
                }

                if (activeRewardsWithdrawalsEnabled.not()) {
                    ActiveRewardsWithdrawalNotice(onLearnMorePressed = withdrawDisabledLearnMore)
                    LargeVerticalSpacer()
                } else {
                    if (state.hasOngoingWithdrawals) {
                        state.balanceCrypto?.currency?.networkTicker?.let {
                            EarnPendingWithdrawalFullBalance(it)
                            LargeVerticalSpacer()
                        }
                    }
                    ActiveRewardsTradingWarning(onLearnMorePressed = withdrawDisabledLearnMore)
                    LargeVerticalSpacer()
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = AppColors.backgroundSecondary,
                        shape = AppTheme.shapes.large.topOnly()
                    )
                    .padding(AppTheme.dimensions.smallSpacing)
            ) {
                SecondaryButton(
                    modifier = Modifier.weight(1F),
                    text = stringResource(id = com.blockchain.stringResources.R.string.common_withdraw),
                    icon = ImageResource.Local(
                        com.blockchain.componentlib.icons.R.drawable.send_off,
                        colorFilter = ColorFilter.tint(Color.White)
                    ),
                    state = if (state.canWithdraw) ButtonState.Enabled else ButtonState.Disabled,
                    onClick = {
                        safeLet(state.account, state.tradingAccount) { account, tradingAccount ->
                            onWithdrawPressed(account, tradingAccount)
                        }
                    }
                )

                TinyHorizontalSpacer()

                SecondaryButton(
                    modifier = Modifier.weight(1F),
                    text = stringResource(id = com.blockchain.stringResources.R.string.common_add),
                    icon = ImageResource.Local(
                        com.blockchain.componentlib.icons.R.drawable.receive_off,
                        colorFilter = ColorFilter.tint(Color.White)
                    ),
                    onClick = { state.account?.let { onDepositPressed(it as EarnRewardsAccount.Active) } },
                    state = if (state.canDeposit) ButtonState.Enabled else ButtonState.Disabled
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewActiveRewardsSummarySheet() {
    AppTheme {
        ActiveRewardsSummarySheet(
            state = ActiveRewardsSummaryViewState(
                account = null, errorState = ActiveRewardsError.None, isLoading = false, balanceCrypto = null,
                tradingAccount = null,
                balanceFiat = null,
                totalEarnedCrypto = null,
                totalEarnedFiat = null, totalSubscribedCrypto = null, totalSubscribedFiat = null,
                totalOnHoldCrypto = null,
                totalOnHoldFiat = null, activeRewardsRate = 0.0, triggerPrice = null,
                rewardsFrequency = EarnRewardsFrequency.Weekly,
                isWithdrawable = false,
                canDeposit = false,
                assetFiatPrice = null,
                canWithdraw = false,
                hasOngoingWithdrawals = false
            ),
            onWithdrawPressed = { _, _ -> },
            onDepositPressed = {},
            withdrawDisabledLearnMore = {},
            onClosePressed = {},
            onExplainerClicked = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewActiveRewardsSummarySheetDark() {
    PreviewActiveRewardsSummarySheet()
}
