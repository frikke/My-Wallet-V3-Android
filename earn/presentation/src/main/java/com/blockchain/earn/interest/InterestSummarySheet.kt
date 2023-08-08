package com.blockchain.earn.interest

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.EarnRewardsAccount
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.SecondaryButton
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.tablerow.custom.TextWithTooltipTableRow
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.LargeVerticalSpacer
import com.blockchain.componentlib.theme.TinyHorizontalSpacer
import com.blockchain.componentlib.theme.TinyVerticalSpacer
import com.blockchain.componentlib.theme.topOnly
import com.blockchain.earn.common.EarnFieldExplainer
import com.blockchain.earn.domain.models.EarnRewardsFrequency
import com.blockchain.earn.interest.viewmodel.InterestError
import com.blockchain.earn.interest.viewmodel.InterestSummaryViewState
import com.blockchain.utils.toFormattedDate
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatCurrency
import info.blockchain.balance.FiatValue
import java.math.BigInteger
import java.util.Calendar

@Composable
fun InterestSummarySheet(
    state: InterestSummaryViewState,
    onWithdrawPressed: (sourceAccount: BlockchainAccount) -> Unit,
    onDepositPressed: (currency: EarnRewardsAccount.Interest) -> Unit,
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
                    id = com.blockchain.stringResources.R.string.passive_rewards_summary_title,
                    state.balanceCrypto?.currency?.networkTicker.orEmpty()
                ),
                startImage = StackedIcon.SingleIcon(
                    ImageResource.Remote(state.balanceCrypto?.currency?.logo.orEmpty())
                ),
                onClosePress = onClosePressed
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F)
                    .padding(horizontal = AppTheme.dimensions.smallSpacing)
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

                        TextWithTooltipTableRow(
                            startText = stringResource(
                                com.blockchain.stringResources.R.string.staking_summary_total_earned
                            ),
                            endTitle = state.totalEarnedFiat?.toStringWithSymbol().orEmpty(),
                            endSubtitle = state.totalEarnedCrypto?.toStringWithSymbol().orEmpty(),
                            isTappable = false
                        )

                        TinyVerticalSpacer()
                        HorizontalDivider(modifier = Modifier.fillMaxWidth())
                        TinyVerticalSpacer()

                        TextWithTooltipTableRow(
                            startText = stringResource(
                                com.blockchain.stringResources.R.string.earn_interest_accrued_this_month
                            ),
                            endTitle = state.pendingInterestFiat?.toStringWithSymbol().orEmpty(),
                            endSubtitle = state.pendingInterestCrypto?.toStringWithSymbol(),
                            onClick = { onExplainerClicked(EarnFieldExplainer.MonthlyAccruedInterest) }
                        )
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
                            startText = stringResource(com.blockchain.stringResources.R.string.rewards_summary_rate),
                            endTitle = "${state.interestRate}%",
                            onClick = { onExplainerClicked(EarnFieldExplainer.InterestRate) }
                        )

                        TinyVerticalSpacer()
                        HorizontalDivider(modifier = Modifier.fillMaxWidth())
                        TinyVerticalSpacer()

                        TextWithTooltipTableRow(
                            startText = stringResource(com.blockchain.stringResources.R.string.earn_payment_frequency),
                            endTitle = when (state.earnFrequency) {
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
                            },
                            onClick = { onExplainerClicked(EarnFieldExplainer.MonthlyPaymentFrequency) }
                        )

                        TinyVerticalSpacer()
                        HorizontalDivider(modifier = Modifier.fillMaxWidth())
                        TinyVerticalSpacer()

                        TextWithTooltipTableRow(
                            startText = stringResource(com.blockchain.stringResources.R.string.common_next_payment),
                            endTitle = "${state.nextPaymentDate?.toFormattedDate()}",
                            isTappable = false
                        )

                        TinyVerticalSpacer()
                        HorizontalDivider(modifier = Modifier.fillMaxWidth())
                        TinyVerticalSpacer()

                        TextWithTooltipTableRow(
                            startText = stringResource(
                                com.blockchain.stringResources.R.string.earn_interest_hold_period
                            ),
                            endTitle = stringResource(
                                com.blockchain.stringResources.R.string.rewards_summary_hold_period_days,
                                state.initialHoldPeriod
                            ),
                            onClick = { onExplainerClicked(EarnFieldExplainer.HoldPeriod) }
                        )

                        TinyVerticalSpacer()
                    }
                }

                LargeVerticalSpacer()
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
                    onClick = { state.account?.let { onWithdrawPressed(it) } }
                )

                TinyHorizontalSpacer()

                SecondaryButton(
                    modifier = Modifier.weight(1F),
                    text = stringResource(id = com.blockchain.stringResources.R.string.common_add),
                    icon = ImageResource.Local(
                        com.blockchain.componentlib.icons.R.drawable.receive_off,
                        colorFilter = ColorFilter.tint(Color.White)
                    ),
                    onClick = { state.account?.let { onDepositPressed(it as EarnRewardsAccount.Interest) } },
                    state = if (state.canDeposit) ButtonState.Enabled else ButtonState.Disabled
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InterestSummarySheetPreview() {
    InterestSummarySheet(
        state = InterestSummaryViewState(
            balanceFiat = FiatValue.fromMinor(FiatCurrency.Dollars, BigInteger("1000000")),
            balanceCrypto = CryptoValue.fromMinor(CryptoCurrency.BTC, BigInteger("1000000")),
            totalEarnedFiat = FiatValue.fromMinor(FiatCurrency.Dollars, BigInteger("1000000")),
            totalEarnedCrypto = CryptoValue.fromMinor(CryptoCurrency.BTC, BigInteger("1000000")),
            pendingInterestFiat = FiatValue.fromMinor(FiatCurrency.Dollars, BigInteger("10000")),
            pendingInterestCrypto = CryptoValue.fromMinor(CryptoCurrency.BTC, BigInteger("10000")),
            interestRate = 0.1,
            nextPaymentDate = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, 3)
            }.time,
            initialHoldPeriod = 7,
            earnFrequency = EarnRewardsFrequency.Monthly,
            account = null,
            errorState = InterestError.None,
            isLoading = false,
            interestCommission = 0.0,
            canWithdraw = false,
            canDeposit = true
        ),
        onWithdrawPressed = {},
        onDepositPressed = {},
        onClosePressed = {},
        onExplainerClicked = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InterestSummarySheetPreviewDark() {
    InterestSummarySheetPreview()
}
