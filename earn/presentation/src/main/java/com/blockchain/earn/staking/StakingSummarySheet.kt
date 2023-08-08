package com.blockchain.earn.staking

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.icon.CustomStackedIcon
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Receive
import com.blockchain.componentlib.icons.Send
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.system.CircularProgressBarWithSmallText
import com.blockchain.componentlib.system.ShimmerLoadingTableRow
import com.blockchain.componentlib.tablerow.FlexibleTableRow
import com.blockchain.componentlib.tablerow.TableRowHeader
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.tablerow.custom.TextWithTooltipTableRow
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.LargeVerticalSpacer
import com.blockchain.componentlib.theme.SmallHorizontalSpacer
import com.blockchain.componentlib.theme.SmallVerticalSpacer
import com.blockchain.componentlib.theme.SmallestVerticalSpacer
import com.blockchain.componentlib.theme.StandardVerticalSpacer
import com.blockchain.componentlib.theme.TinyHorizontalSpacer
import com.blockchain.componentlib.theme.TinyVerticalSpacer
import com.blockchain.componentlib.theme.topOnly
import com.blockchain.earn.common.EarnFieldExplainer
import com.blockchain.earn.domain.models.EarnRewardsFrequency
import com.blockchain.earn.domain.models.staking.StakingActivityType
import com.blockchain.earn.staking.viewmodel.StakingActivityViewState
import com.blockchain.earn.staking.viewmodel.StakingError
import com.blockchain.earn.staking.viewmodel.StakingSummaryViewState
import com.blockchain.extensions.safeLet
import com.blockchain.stringResources.R
import kotlinx.coroutines.delay

private val List<StakingActivityViewState>.withdrawals: List<StakingActivityViewState>
    get() = filter { it.type == StakingActivityType.Unbonding }

@Composable
fun StakingSummarySheet(
    state: StakingSummaryViewState,
    onWithdrawPressed: (sourceAccount: BlockchainAccount, targetAccount: CustodialTradingAccount) -> Unit,
    onDepositPressed: (currency: EarnRewardsAccount.Staking) -> Unit,
    withdrawDisabledLearnMore: () -> Unit,
    onExplainerClicked: (EarnFieldExplainer) -> Unit,
    onClosePressed: () -> Unit
) {
    var withdrawalsLocked by remember { mutableStateOf(false) }

    Surface(
        color = AppColors.background,
        shape = AppTheme.shapes.large.topOnly()
    ) {
        Column {
            SheetHeader(
                title = stringResource(
                    id = R.string.staking_summary_title,
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

                        safeLet(state.earnedFiat, state.earnedCrypto) { earnedFiat, earnedCrypto ->
                            TextWithTooltipTableRow(
                                startText = stringResource(
                                    R.string.staking_summary_total_earned
                                ),
                                endTitle = earnedFiat.toStringWithSymbol(),
                                endSubtitle = earnedCrypto.toStringWithSymbol()
                            )

                            TinyVerticalSpacer()
                            HorizontalDivider(modifier = Modifier.fillMaxWidth())
                            TinyVerticalSpacer()
                        }

                        TextWithTooltipTableRow(
                            startText = stringResource(R.string.rewards_summary_rate),
                            endTitle = "${state.stakingRate}%",
                            onClick = {
                                onExplainerClicked(EarnFieldExplainer.StakingEarnRate)
                            }
                        )

                        TinyVerticalSpacer()
                        HorizontalDivider(modifier = Modifier.fillMaxWidth())
                        TinyVerticalSpacer()

                        TextWithTooltipTableRow(
                            startText = stringResource(R.string.earn_payment_frequency),
                            endTitle = when (state.earnFrequency) {
                                EarnRewardsFrequency.Daily ->
                                    stringResource(
                                        id = R.string.earn_payment_frequency_daily
                                    )

                                EarnRewardsFrequency.Weekly ->
                                    stringResource(
                                        id = R.string.earn_payment_frequency_weekly
                                    )

                                EarnRewardsFrequency.Monthly ->
                                    stringResource(
                                        id = R.string.earn_payment_frequency_monthly
                                    )

                                else ->
                                    stringResource(
                                        id = R.string.earn_payment_frequency_unknown
                                    )
                            }
                        )

                        TinyVerticalSpacer()
                    }
                }

                LargeVerticalSpacer()

                if (state.shouldShowWithdrawWarning()) {
                    StakingWithdrawalNotice(onLearnMorePressed = withdrawDisabledLearnMore)
                } else {
                    if (state.pendingActivity.isNotEmpty()) {
                        PendingActivity(pendingActivity = state.pendingActivity)
                        StandardVerticalSpacer()
                    }

                    StakingWithdrawalQueueNotice(
                        unbondingDays = state.unbondingDays,
                        onLearnMorePressed = withdrawDisabledLearnMore
                    )
                    LargeVerticalSpacer()
                }

                LargeVerticalSpacer()

                if (state.pendingActivity.withdrawals.isNotEmpty()) {
                    val withdrawalTimestamp = state.pendingActivity.withdrawals.last().timestamp?.time ?: 0L
                    val timeBetweenWithdrawals = 5 * 60 * 1000 // 5 minutes in milliseconds
                    val unlockTime = withdrawalTimestamp + timeBetweenWithdrawals

                    val timeUntilUnlock = remember {
                        mutableLongStateOf(unlockTime - System.currentTimeMillis())
                    }

                    LaunchedEffect(Unit) {
                        withdrawalsLocked = true
                        while (true) {
                            delay(1000L)
                            val newTimeUntilUnlock = unlockTime - System.currentTimeMillis()
                            if (newTimeUntilUnlock != timeUntilUnlock.longValue) {
                                timeUntilUnlock.longValue = newTimeUntilUnlock
                            }
                        }
                    }

                    if (timeUntilUnlock.longValue > 0) {
                        val progress = (timeUntilUnlock.longValue.toFloat() / timeBetweenWithdrawals.toFloat())
                            .coerceIn(0f, 1f)
                        CircularProgressBarWithSmallText(
                            progress = progress,
                            text = stringResource(
                                id =
                                R
                                    .string.earn_staking_withdrawal_locked_countdown_message,
                                formatDuration(timeUntilUnlock.longValue)
                            )
                        )
                        SmallVerticalSpacer()
                    } else {
                        withdrawalsLocked = false
                    }
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
                    text = stringResource(id = R.string.common_withdraw),
                    icon = ImageResource.Local(
                        com.blockchain.componentlib.icons.R.drawable.send_off,
                        colorFilter = ColorFilter.tint(Color.White)
                    ),
                    state = if (state.canWithdraw && !withdrawalsLocked) {
                        ButtonState.Enabled
                    } else ButtonState.Disabled,
                    onClick = {
                        safeLet(state.account, state.tradingAccount) { account, tradingAccount ->
                            onWithdrawPressed(account, tradingAccount)
                        }
                    }
                )

                TinyHorizontalSpacer()

                SecondaryButton(
                    modifier = Modifier.weight(1F),
                    text = stringResource(id = R.string.common_add),
                    icon = ImageResource.Local(
                        com.blockchain.componentlib.icons.R.drawable.receive_off,
                        colorFilter = ColorFilter.tint(Color.White)
                    ),
                    onClick = { state.account?.let { onDepositPressed(it as EarnRewardsAccount.Staking) } },
                    state = if (state.canDeposit) ButtonState.Enabled else ButtonState.Disabled
                )
            }
        }
    }
}

@Composable
fun StakingWithdrawalNotice(onLearnMorePressed: () -> Unit) {
    Card(
        backgroundColor = AppTheme.colors.backgroundSecondary,
        shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
        border = BorderStroke(
            width = 1.dp,
            color = AppTheme.colors.warning
        ),
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.smallSpacing)
        ) {
            SimpleText(
                text = stringResource(com.blockchain.stringResources.R.string.common_important_information),
                style = ComposeTypographies.Paragraph2,
                color = ComposeColors.Warning,
                gravity = ComposeGravities.Start
            )

            TinyVerticalSpacer()

            SimpleText(
                text = stringResource(com.blockchain.stringResources.R.string.earn_staking_withdrawal_blocked),
                style = ComposeTypographies.Caption1,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Start
            )

            SmallVerticalSpacer()

            SecondaryButton(
                text = stringResource(id = com.blockchain.stringResources.R.string.common_learn_more),
                onClick = onLearnMorePressed
            )
        }
    }
}

@Composable
private fun PendingActivity(
    pendingActivity: List<StakingActivityViewState>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TableRowHeader(title = stringResource(id = R.string.common_pending_activity))
        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
            color = Color.Transparent
        ) {
            Column {
                pendingActivity.forEach { activity ->
                    FlexibleTableRow(
                        paddingValues = PaddingValues(AppTheme.dimensions.smallSpacing),
                        contentStart = {
                            CustomStackedIcon(
                                icon = StackedIcon.SingleIcon(
                                    when (activity.type) {
                                        StakingActivityType.Bonding -> Icons.Receive
                                        StakingActivityType.Unbonding -> Icons.Send
                                    }.withTint(AppColors.title)
                                )
                            )
                        },
                        content = {
                            SmallHorizontalSpacer()

                            Column {
                                SimpleText(
                                    text = stringResource(
                                        when (activity.type) {
                                            StakingActivityType.Bonding -> R.string.earn_staking_bonding_asset
                                            StakingActivityType.Unbonding -> R.string.earn_staking_unbonding_asset
                                        },
                                        activity.currency
                                    ),
                                    style = ComposeTypographies.Body2,
                                    color = ComposeColors.Title,
                                    gravity = ComposeGravities.Start
                                )

                                SmallestVerticalSpacer()

                                SimpleText(
                                    text = stringResource(
                                        when (activity.type) {
                                            StakingActivityType.Bonding -> R.string.earn_staking_bonding_days
                                            StakingActivityType.Unbonding -> R.string.earn_staking_unbonding_days
                                        },
                                        activity.durationDays
                                    ),
                                    style = ComposeTypographies.Paragraph1,
                                    color = ComposeColors.Primary,
                                    gravity = ComposeGravities.Start
                                )
                            }
                            SmallHorizontalSpacer()
                        },
                        contentEnd = {
                            Column(horizontalAlignment = Alignment.End) {
                                SimpleText(
                                    text = activity.expiryDate,
                                    style = ComposeTypographies.Caption1,
                                    color = ComposeColors.Body,
                                    gravity = ComposeGravities.End
                                )

                                SmallestVerticalSpacer()

                                SimpleText(
                                    text = activity.amountCrypto,
                                    style = ComposeTypographies.Caption1,
                                    color = ComposeColors.Body,
                                    gravity = ComposeGravities.End
                                )
                            }
                        },
                        onContentClicked = {}
                    )
                }
            }
        }
    }
}

@Composable
fun StakingWithdrawalQueueNotice(unbondingDays: Int, onLearnMorePressed: () -> Unit) {
    Card(
        backgroundColor = AppTheme.colors.backgroundSecondary,
        shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.smallSpacing)
        ) {
            SimpleText(
                text = stringResource(
                    com.blockchain.stringResources.R.string.earn_staking_withdrawal_queue_notice_title
                ),
                style = ComposeTypographies.Paragraph2,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Start
            )

            TinyVerticalSpacer()

            SimpleText(
                text = stringResource(
                    com.blockchain.stringResources.R.string.earn_staking_withdrawal_queue_notice_description,
                    unbondingDays
                ),
                style = ComposeTypographies.Caption1,
                color = ComposeColors.Title,
                gravity = ComposeGravities.Start
            )

            SmallVerticalSpacer()

            SecondaryButton(
                text = stringResource(id = com.blockchain.stringResources.R.string.common_learn_more),
                onClick = onLearnMorePressed
            )
        }
    }
}

@Composable
fun SummarySheetLoading() {
    Column {
        ShimmerLoadingTableRow(false)
        ShimmerLoadingTableRow(false)
        ShimmerLoadingTableRow(false)
    }
}

private fun StakingSummaryViewState.shouldShowWithdrawWarning(): Boolean =
    // TODO (labreu): remove hardcoded ETH once staking is available for other networks
    !canWithdraw && balanceCrypto?.currency?.networkTicker == "ETH"

private fun formatDuration(duration: Long): String {
    val minutes = (duration / 1000) / 60
    val seconds = (duration / 1000) % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Preview
@Composable
fun StakingSummarySheetPreview() {
    StakingSummarySheet(
        state = StakingSummaryViewState(
            account = null,
            tradingAccount = null,
            errorState = StakingError.None,
            isLoading = false,
            balanceCrypto = null,
            balanceFiat = null,
            stakedCrypto = null,
            stakedFiat = null,
            bondingCrypto = null,
            bondingFiat = null,
            earnedCrypto = null,
            earnedFiat = null,
            stakingRate = 5.0,
            commissionRate = 1.0,
            earnFrequency = EarnRewardsFrequency.Weekly,
            canDeposit = true,
            canWithdraw = true,
            pendingActivity = previewPendingActivity,
            unbondingDays = 2
        ),
        onWithdrawPressed = { _, _ -> },
        onDepositPressed = {},
        withdrawDisabledLearnMore = {},
        onClosePressed = {},
        onExplainerClicked = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StakingSummarySheetPreviewDark() {
    StakingSummarySheetPreview()
}

@Preview
@Composable
fun StakingWithdrawalNoticePreview() {
    StakingWithdrawalNotice(onLearnMorePressed = {})
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StakingWithdrawalNoticePreviewDark() {
    StakingWithdrawalNoticePreview()
}

@Preview(showBackground = true)
@Composable
fun PreviewEarnPendingWithdrawals() {
    PendingActivity(
        pendingActivity = previewPendingActivity
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewEarnPendingWithdrawalsDark() {
    PreviewEarnPendingWithdrawals()
}

private val previewPendingActivity = listOf(
    StakingActivityViewState(
        currency = "BTC",
        amountCrypto = "-0.00000001 BTC",
        amountFiat = "-£0.01",
        startDate = "2021-05-01",
        expiryDate = "2021-05-02",
        timestamp = null,
        durationDays = 5,
        type = StakingActivityType.Bonding
    ),
    StakingActivityViewState(
        currency = "BTC",
        amountCrypto = "-0.00000001 BTC",
        amountFiat = "-£0.01",
        startDate = "2021-05-01",
        expiryDate = "2021-05-02",
        timestamp = null,
        durationDays = 5,
        type = StakingActivityType.Unbonding

    ),
    StakingActivityViewState(
        currency = "BTC",
        amountCrypto = "-0.00000001 BTC",
        amountFiat = "-£0.01",
        startDate = "2021-05-01",
        expiryDate = "2021-05-02",
        timestamp = null,
        durationDays = 5,
        type = StakingActivityType.Bonding
    )
)
