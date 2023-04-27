package com.blockchain.earn.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.icon.CustomStackedIcon
import com.blockchain.componentlib.tablerow.FlexibleTableRow
import com.blockchain.componentlib.tablerow.TableRowHeader
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.SmallHorizontalSpacer
import com.blockchain.componentlib.theme.SmallestVerticalSpacer
import com.blockchain.earn.R
import com.blockchain.earn.staking.viewmodel.EarnWithdrawalUiElement

@Composable
fun EarnPendingWithdrawalFullBalance(currencyTicker: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TableRowHeader(title = stringResource(id = R.string.common_pending_activity))
        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
            elevation = 0.dp,
        ) {
            FlexibleTableRow(
                paddingValues = PaddingValues(AppTheme.dimensions.smallSpacing),
                contentStart = {
                    CustomStackedIcon(icon = StackedIcon.SingleIcon(ImageResource.Local(R.drawable.send_off)))
                },
                content = {
                    SmallHorizontalSpacer()

                    Column {
                        SimpleText(
                            text = stringResource(R.string.earn_active_rewards_withdrawal_activity, currencyTicker),
                            style = ComposeTypographies.Body2,
                            color = ComposeColors.Title,
                            gravity = ComposeGravities.Start
                        )

                        SmallestVerticalSpacer()

                        SimpleText(
                            text = stringResource(R.string.common_requested),
                            style = ComposeTypographies.Paragraph1,
                            color = ComposeColors.Primary,
                            gravity = ComposeGravities.Start
                        )
                    }

                    SimpleText(
                        text = stringResource(R.string.earn_active_rewards_withdrawal_close_date),
                        style = ComposeTypographies.Caption1,
                        color = ComposeColors.Body,
                        gravity = ComposeGravities.End,
                        modifier = Modifier.weight(1F)
                    )

                    SmallHorizontalSpacer()
                },
                onContentClicked = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewEarnPendingWithdrawalFullBalance() {
    AppTheme {
        EarnPendingWithdrawalFullBalance(currencyTicker = "BTC")
    }
}

@Composable
fun EarnPendingWithdrawals(pendingWithdrawals: List<EarnWithdrawalUiElement>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TableRowHeader(title = stringResource(id = R.string.common_pending_activity))
        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
            elevation = 0.dp,
        ) {

            Column {
                pendingWithdrawals.forEach { pendingWithdrawal ->
                    FlexibleTableRow(
                        paddingValues = PaddingValues(AppTheme.dimensions.smallSpacing),
                        contentStart = {
                            CustomStackedIcon(icon = StackedIcon.SingleIcon(ImageResource.Local(R.drawable.send_off)))
                        },
                        content = {
                            SmallHorizontalSpacer()

                            Column {
                                SimpleText(
                                    text = "Withdrew ${pendingWithdrawal.currency}",
                                    style = ComposeTypographies.Body2,
                                    color = ComposeColors.Title,
                                    gravity = ComposeGravities.Start
                                )

                                SmallestVerticalSpacer()

                                SimpleText(
                                    text = "Unbonding",
                                    style = ComposeTypographies.Paragraph1,
                                    color = ComposeColors.Primary,
                                    gravity = ComposeGravities.Start
                                )
                            }
                            SmallHorizontalSpacer()
                        },
                        contentEnd = {
                            Column {
                                SimpleText(
                                    text = pendingWithdrawal.unbondingStartDate,
                                    style = ComposeTypographies.Caption1,
                                    color = ComposeColors.Body,
                                    gravity = ComposeGravities.End,
                                )

                                SmallestVerticalSpacer()

                                SimpleText(
                                    text = pendingWithdrawal.amountCrypto,
                                    style = ComposeTypographies.Caption1,
                                    color = ComposeColors.Body,
                                    gravity = ComposeGravities.End,
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

@Preview(showBackground = true)
@Composable
fun PreviewEarnPendingWithdrawals() {
    AppTheme {
        EarnPendingWithdrawals(
            pendingWithdrawals = listOf(
                EarnWithdrawalUiElement(
                    currency = "BTC",
                    amountCrypto = "-0.00000001 BTC",
                    amountFiat = "-£0.01",
                    unbondingStartDate = "2021-05-01",
                    unbondingExpiryDate = "2021-05-02",
                    null
                ),
                EarnWithdrawalUiElement(
                    currency = "BTC",
                    amountCrypto = "-0.00000001 BTC",
                    amountFiat = "-£0.01",
                    unbondingStartDate = "2021-05-01",
                    unbondingExpiryDate = "2021-05-02",
                    null
                ),
                EarnWithdrawalUiElement(
                    currency = "BTC",
                    amountCrypto = "-0.00000001 BTC",
                    amountFiat = "-£0.01",
                    unbondingStartDate = "2021-05-01",
                    unbondingExpiryDate = "2021-05-02",
                    null
                )
            )
        )
    }
}
