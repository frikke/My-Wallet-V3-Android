package com.blockchain.earn.common

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.icon.CustomStackedIcon
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Send
import com.blockchain.componentlib.tablerow.FlexibleTableRow
import com.blockchain.componentlib.tablerow.TableRowHeader
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.SmallHorizontalSpacer
import com.blockchain.componentlib.theme.SmallestVerticalSpacer

@Composable
fun EarnPendingWithdrawalFullBalance(currencyTicker: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TableRowHeader(title = stringResource(id = com.blockchain.stringResources.R.string.common_pending_activity))
        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
            color = Color.Transparent
        ) {
            FlexibleTableRow(
                paddingValues = PaddingValues(AppTheme.dimensions.smallSpacing),
                contentStart = {
                    CustomStackedIcon(
                        icon = StackedIcon.SingleIcon(
                            Icons.Send.withTint(AppColors.title)
                        )
                    )
                },
                content = {
                    SmallHorizontalSpacer()

                    Column {
                        SimpleText(
                            text = stringResource(
                                com.blockchain.stringResources.R.string.earn_active_rewards_withdrawal_activity,
                                currencyTicker
                            ),
                            style = ComposeTypographies.Body2,
                            color = ComposeColors.Title,
                            gravity = ComposeGravities.Start
                        )

                        SmallestVerticalSpacer()

                        SimpleText(
                            text = stringResource(com.blockchain.stringResources.R.string.common_requested),
                            style = ComposeTypographies.Paragraph1,
                            color = ComposeColors.Primary,
                            gravity = ComposeGravities.Start
                        )
                    }

                    SimpleText(
                        text = stringResource(
                            com.blockchain.stringResources.R.string.earn_active_rewards_withdrawal_close_date
                        ),
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

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewEarnPendingWithdrawalFullBalanceDark() {
    PreviewEarnPendingWithdrawalFullBalance()
}
