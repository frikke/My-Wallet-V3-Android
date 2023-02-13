package com.blockchain.earn.staking

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.coincore.EarnRewardsAccount
import com.blockchain.componentlib.alert.CardAlert
import com.blockchain.componentlib.alert.SnackbarAlert
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.ButtonState
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.button.SecondaryButton
import com.blockchain.componentlib.card.ButtonType
import com.blockchain.componentlib.card.CardButton
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.divider.VerticalDivider
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.system.ShimmerLoadingTableRow
import com.blockchain.componentlib.tablerow.BalanceTableRow
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.earn.EarnAnalytics
import com.blockchain.earn.R
import com.blockchain.earn.dashboard.viewmodel.EarnType
import com.blockchain.earn.domain.models.EarnRewardsFrequency
import com.blockchain.earn.staking.viewmodel.StakingError
import com.blockchain.earn.staking.viewmodel.StakingSummaryViewState

private sealed class InfoSnackbarState {
    object Hidden : InfoSnackbarState()
    object BondingInfo : InfoSnackbarState()
    object RateInfo : InfoSnackbarState()
}

@Composable
fun StakingSummarySheet(
    state: StakingSummaryViewState,
    onWithdrawPressed: (currency: EarnRewardsAccount.Staking) -> Unit,
    onDepositPressed: (currency: EarnRewardsAccount.Staking) -> Unit,
    withdrawDisabledLearnMore: () -> Unit,
    onClosePressed: () -> Unit,
) {
    val hasDepositsBonding: Boolean = remember { state.bondingCrypto?.isPositive == true }
    var snackbarState by remember { mutableStateOf<InfoSnackbarState>(InfoSnackbarState.Hidden) }

    Box {

        Column {
            SheetHeader(
                title = stringResource(
                    id = R.string.staking_summary_title, state.balanceCrypto?.currency?.networkTicker.orEmpty()
                ),
                startImageResource = ImageResource.Remote(state.balanceCrypto?.currency?.logo.orEmpty()),
                shouldShowDivider = false,
                onClosePress = onClosePressed
            )

            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                Row(
                    modifier = Modifier
                        .padding(
                            start = dimensionResource(id = R.dimen.small_spacing),
                            end = dimensionResource(id = R.dimen.small_spacing),
                            top = dimensionResource(id = R.dimen.tiny_spacing),
                            bottom = dimensionResource(id = R.dimen.tiny_spacing)
                        )
                        .wrapContentHeight()

                ) {

                    StakingSummaryBalanceHeader(
                        modifier = Modifier.weight(1f),
                        title = R.string.common_balance,
                        fiatValue = state.balanceFiat?.toStringWithSymbol().orEmpty(),
                        cryptoValue = state.balanceCrypto?.toStringWithSymbol().orEmpty(),
                    )

                    VerticalDivider(
                        modifier = Modifier
                            .height(dimensionResource(R.dimen.xhuge_spacing))
                            .padding(
                                end = dimensionResource(id = R.dimen.medium_spacing),
                                start = dimensionResource(id = R.dimen.medium_spacing)
                            )
                            .align(Alignment.CenterVertically),
                        dividerColor = AppTheme.colors.medium
                    )

                    StakingSummaryBalanceHeader(
                        modifier = Modifier.weight(1f),
                        title = R.string.staking_summary_total_earned,
                        fiatValue = state.earnedFiat?.toStringWithSymbol().orEmpty(),
                        cryptoValue = state.earnedCrypto?.toStringWithSymbol().orEmpty(),
                    )
                }

                BalanceTableRow(
                    titleStart = buildAnnotatedString {
                        append(stringResource(id = R.string.staking_summary_total_staked))
                    },
                    titleEnd = buildAnnotatedString {
                        append(state.stakedFiat?.toStringWithSymbol().orEmpty())
                    },
                    bodyEnd = buildAnnotatedString {
                        append(state.stakedCrypto?.toStringWithSymbol().orEmpty())
                    }
                )

                HorizontalDivider(modifier = Modifier.fillMaxWidth(), dividerColor = AppTheme.colors.medium)

                if (hasDepositsBonding) {
                    BalanceTableRow(
                        titleStart = buildAnnotatedString {
                            append(stringResource(id = R.string.staking_summary_total_bonding))
                        },
                        titleEnd = buildAnnotatedString {
                            append(state.bondingFiat?.toStringWithSymbol().orEmpty())
                        },
                        bodyEnd = buildAnnotatedString {
                            append(state.bondingCrypto?.toStringWithSymbol().orEmpty())
                        },
                        postStartTitleImageResource = ImageResource.Local(R.drawable.ic_info),
                        postStartTitleImageResourceOnClick = {
                            snackbarState = InfoSnackbarState.BondingInfo
                        }
                    )

                    HorizontalDivider(modifier = Modifier.fillMaxWidth(), dividerColor = AppTheme.colors.medium)
                }

                BalanceTableRow(
                    titleStart = buildAnnotatedString {
                        append(stringResource(id = R.string.staking_summary_rate))
                    },
                    titleEnd = buildAnnotatedString {
                        append(stringResource(R.string.staking_summary_rate_value, state.stakingRate.toString()))
                    },
                    postStartTitleImageResource = ImageResource.Local(R.drawable.ic_info),
                    postStartTitleImageResourceOnClick = {
                        snackbarState = InfoSnackbarState.RateInfo
                    }
                )

                HorizontalDivider(modifier = Modifier.fillMaxWidth(), dividerColor = AppTheme.colors.medium)

                BalanceTableRow(
                    titleStart = buildAnnotatedString {
                        append(stringResource(id = R.string.staking_summary_payment_frequency))
                    },
                    titleEnd = buildAnnotatedString {
                        append(
                            when (state.rewardsFrequency) {
                                EarnRewardsFrequency.Daily -> stringResource(
                                    R.string.staking_summary_payment_frequency_daily
                                )
                                EarnRewardsFrequency.Weekly -> stringResource(
                                    R.string.staking_summary_payment_frequency_weekly
                                )
                                EarnRewardsFrequency.Monthly -> stringResource(
                                    R.string.staking_summary_payment_frequency_monthly
                                )
                                EarnRewardsFrequency.Unknown -> stringResource(
                                    R.string.staking_summary_payment_frequency_unknown
                                )
                            }
                        )
                    }
                )

                if (state.shouldShowWithdrawWarning()) {
                    HorizontalDivider(modifier = Modifier.fillMaxWidth(), dividerColor = AppTheme.colors.medium)

                    Box(modifier = Modifier.padding(dimensionResource(id = R.dimen.small_spacing))) {
                        CardAlert(
                            title = stringResource(id = R.string.empty),
                            subtitle = stringResource(id = R.string.staking_summary_non_withdrawable_info),
                            isBordered = false,
                            isDismissable = false,
                            primaryCta = CardButton(
                                text = stringResource(id = R.string.common_learn_more),
                                type = ButtonType.Minimal,
                                onClick = withdrawDisabledLearnMore
                            )
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        bottom = dimensionResource(id = R.dimen.small_spacing),
                        start = dimensionResource(id = R.dimen.small_spacing),
                        end = dimensionResource(id = R.dimen.small_spacing)
                    )
            ) {
                SecondaryButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.common_withdraw),
                    onClick = {
                        state.account?.let {
                            onWithdrawPressed(it)
                        }

                        state.balanceCrypto?.let {
                            EarnAnalytics.WithdrawClicked(
                                currency = it.currency.networkTicker,
                                product = EarnType.Staking
                            )
                        }
                    },
                    state = if (state.isWithdrawable && (state.balanceCrypto?.isPositive) == true) {
                        ButtonState.Enabled
                    } else {
                        ButtonState.Disabled
                    },
                    icon = ImageResource.Local(R.drawable.ic_withdraw)
                )

                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.tiny_spacing)))

                PrimaryButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.common_add),
                    onClick = {
                        state.account?.let {
                            onDepositPressed(it)
                        }

                        state.balanceCrypto?.let {
                            EarnAnalytics.AddClicked(
                                currency = it.currency.networkTicker,
                                product = EarnType.Staking
                            )
                        }
                    },
                    state = if (state.canDeposit) ButtonState.Enabled else ButtonState.Disabled,
                    icon = ImageResource.Local(R.drawable.ic_deposit)
                )
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
                            R.string.staking_summary_rate_explanation, state.commissionRate.toString()
                        ),
                        actionLabel = stringResource(R.string.common_ok),
                        onActionClicked = {
                            snackbarState = InfoSnackbarState.Hidden
                        }
                    )
                }
                is InfoSnackbarState.BondingInfo -> {
                    SnackbarAlert(
                        message = stringResource(R.string.staking_summary_bonding_explanation),
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

@Composable
fun StakingSummaryBalanceHeader(
    @StringRes title: Int,
    fiatValue: String,
    cryptoValue: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        SimpleText(
            text = stringResource(title),
            style = ComposeTypographies.Caption1,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Start
        )

        SimpleText(
            modifier = Modifier.padding(
                top = dimensionResource(id = R.dimen.tiny_spacing),
            ),
            text = fiatValue,
            style = ComposeTypographies.Title2,
            color = ComposeColors.Title,
            gravity = ComposeGravities.Start
        )

        SimpleText(
            modifier = Modifier.padding(
                top = dimensionResource(id = R.dimen.tiny_spacing),
            ),
            text = cryptoValue,
            style = ComposeTypographies.Paragraph1,
            color = ComposeColors.Muted,
            gravity = ComposeGravities.Start
        )
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
    !isWithdrawable && balanceCrypto?.currency?.networkTicker == "ETH"

@Preview
@Composable
fun StakingSummaryPreview() {
    AppSurface {
        AppTheme {
            StakingSummarySheet(
                StakingSummaryViewState(
                    account = null,
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
                    isWithdrawable = false,
                    rewardsFrequency = EarnRewardsFrequency.Weekly,
                    canDeposit = false
                ),
                {},
                {},
                {},
                {},
            )
        }
    }
}
