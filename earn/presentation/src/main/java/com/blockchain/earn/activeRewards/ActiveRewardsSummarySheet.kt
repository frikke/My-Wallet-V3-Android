package com.blockchain.earn.activeRewards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
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
import com.blockchain.componentlib.alert.SnackbarAlert
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.SecondaryButton
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.tablerow.custom.TextWithTooltipTableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.LargeVerticalSpacer
import com.blockchain.componentlib.theme.TinyHorizontalSpacer
import com.blockchain.componentlib.theme.TinyVerticalSpacer
import com.blockchain.earn.R
import com.blockchain.earn.activeRewards.viewmodel.ActiveRewardsError
import com.blockchain.earn.activeRewards.viewmodel.ActiveRewardsSummaryViewState
import com.blockchain.earn.domain.models.EarnRewardsFrequency
import com.blockchain.extensions.safeLet
import com.blockchain.featureflag.FeatureFlag
import com.blockchain.koin.activeRewardsWithdrawalsFeatureFlag
import org.koin.androidx.compose.get

private sealed class InfoSnackbarState {
    object Hidden : InfoSnackbarState()
    object BondingInfo : InfoSnackbarState()
    object RateInfo : InfoSnackbarState()
}

@Composable
fun ActiveRewardsSummarySheet(
    state: ActiveRewardsSummaryViewState,
    onWithdrawPressed: (sourceAccount: BlockchainAccount, targetAccount: CustodialTradingAccount) -> Unit,
    onDepositPressed: (currency: EarnRewardsAccount.Active) -> Unit,
    withdrawDisabledLearnMore: () -> Unit,
    onClosePressed: () -> Unit,
) {

    var snackbarState by remember { mutableStateOf<InfoSnackbarState>(InfoSnackbarState.Hidden) }
    Box {
        Column {
            SheetHeader(
                title = stringResource(
                    id = R.string.active_rewards_summary_title,
                    state.balanceCrypto?.currency?.networkTicker.orEmpty()
                ),
                startImageResource = ImageResource.Remote(state.balanceCrypto?.currency?.logo.orEmpty()),
                shouldShowDivider = false,
                onClosePress = onClosePressed
            )

            Column(
                modifier = Modifier
                    .background(color = AppTheme.colors.light)
                    .fillMaxWidth()
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
                    backgroundColor = AppTheme.colors.background,
                    shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
                    elevation = 0.dp,
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TinyVerticalSpacer()

                        safeLet(
                            state.balanceCrypto?.currency?.networkTicker,
                            state.assetFiatPrice
                        ) { currencyTicker, assetFiatPrice ->
                            TextWithTooltipTableRow(
                                startText = stringResource(
                                    id = R.string.quote_price,
                                    currencyTicker
                                ),
                                endTitle = assetFiatPrice.toStringWithSymbol(),
                            )
                        }

                        if (state.totalEarnedFiat != null && state.totalEarnedCrypto != null) {
                            TextWithTooltipTableRow(
                                startText = stringResource(R.string.earn_net_earnings),
                                endTitle = state.totalEarnedFiat.toStringWithSymbol(),
                                endSubtitle = state.totalEarnedCrypto.toStringWithSymbol(),
                                isTappable = true,
                                tooltipContent = {
                                    TooltipText(tooltipText = stringResource(R.string.earn_net_earning_explainer))
                                }
                            )
                        }

                        if (state.totalSubscribedFiat != null && state.totalSubscribedCrypto != null) {
                            TextWithTooltipTableRow(
                                startText = stringResource(R.string.earn_total_subscribed),
                                endTitle = state.totalSubscribedFiat.toStringWithSymbol(),
                                endSubtitle = state.totalSubscribedCrypto.toStringWithSymbol(),
                            )
                        }

                        if (state.totalOnHoldFiat != null && state.totalOnHoldCrypto != null) {
                            TextWithTooltipTableRow(
                                startText = stringResource(R.string.earn_on_hold),
                                endTitle = state.totalOnHoldFiat.toStringWithSymbol(),
                                endSubtitle = state.totalOnHoldCrypto.toStringWithSymbol(),
                                isTappable = true,
                                tooltipContent = {
                                    TooltipText(tooltipText = stringResource(R.string.earn_on_hold_explainer))
                                }
                            )
                        }
                        TinyVerticalSpacer()
                    }
                }

                LargeVerticalSpacer()

                Card(
                    backgroundColor = AppTheme.colors.background,
                    shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
                    elevation = 0.dp,
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TinyVerticalSpacer()

                        TextWithTooltipTableRow(
                            startText = stringResource(R.string.earn_annual_rate),
                            endTitle = "${state.activeRewardsRate}%",
                            isTappable = true,
                            tooltipContent = {
                                TooltipText(tooltipText = stringResource(R.string.earn_annual_rate_explainer))
                            }
                        )

                        if (state.triggerPrice != null) {
                            TextWithTooltipTableRow(
                                startText = stringResource(R.string.earn_trigger_price),
                                endTitle = state.triggerPrice.toStringWithSymbol(),
                                isTappable = true,
                                tooltipContent = {
                                    TooltipText(tooltipText = stringResource(R.string.earn_trigger_price_explainer))
                                }
                            )
                        }

                        TextWithTooltipTableRow(
                            startText = stringResource(R.string.earn_payment_frequency),
                            endTitle = when (state.rewardsFrequency) {
                                EarnRewardsFrequency.Daily ->
                                    stringResource(id = R.string.earn_payment_frequency_daily)

                                EarnRewardsFrequency.Weekly ->
                                    stringResource(id = R.string.earn_payment_frequency_weekly)

                                EarnRewardsFrequency.Monthly ->
                                    stringResource(id = R.string.earn_payment_frequency_monthly)

                                else ->
                                    stringResource(id = R.string.earn_payment_frequency_unknown)
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
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    SecondaryButton(
                        modifier = Modifier.weight(1F),
                        text = stringResource(id = R.string.common_withdraw),
                        icon = ImageResource.Local(R.drawable.send_off, colorFilter = ColorFilter.tint(Color.White)),
                        state = if (state.canWithdraw) ButtonState.Enabled else ButtonState.Disabled,
                        onClick = {
                            safeLet(state.account, state.tradingAccount) { account, tradingAccount ->
                                onWithdrawPressed(account, tradingAccount)
                            }
                        },
                    )

                    TinyHorizontalSpacer()

                    SecondaryButton(
                        modifier = Modifier.weight(1F),
                        text = stringResource(id = R.string.common_add),
                        icon = ImageResource.Local(R.drawable.receive_off, colorFilter = ColorFilter.tint(Color.White)),
                        onClick = { state.account?.let { onDepositPressed(it as EarnRewardsAccount.Active) } }
                    )
                }

                LargeVerticalSpacer()
            }
        }

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = snackbarState !is InfoSnackbarState.Hidden,
            enter = slideInHorizontally() + fadeIn(),
            exit = slideOutHorizontally() + fadeOut()
        ) {
            when (snackbarState) {
                is InfoSnackbarState.RateInfo -> {
                    SnackbarAlert(
                        message = stringResource(
                            R.string.earn_rate_explanation, state.activeRewardsRate.toString()
                        ),
                        actionLabel = stringResource(R.string.common_ok),
                        onActionClicked = {
                            snackbarState = InfoSnackbarState.Hidden
                        }
                    )
                }
                else -> {
                    // do nothing
                }
            }
        }
    }
}

@Preview(showBackground = true)
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
                canWithdraw = false
            ),
            onWithdrawPressed = { _, _ -> },
            onDepositPressed = {},
            withdrawDisabledLearnMore = {},
            onClosePressed = {},
        )
    }
}

@Composable
fun TooltipText(tooltipText: String) {
    SimpleText(
        text = tooltipText,
        style = ComposeTypographies.Paragraph1,
        color = ComposeColors.Body,
        gravity = ComposeGravities.Start,
        modifier = Modifier.padding(top = AppTheme.dimensions.smallestSpacing)
    )
}
