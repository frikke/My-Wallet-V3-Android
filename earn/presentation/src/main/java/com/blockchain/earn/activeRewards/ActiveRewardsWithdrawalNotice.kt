package com.blockchain.earn.activeRewards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.TinyVerticalSpacer
import com.blockchain.earn.R

@Composable
fun ActiveRewardsWithdrawalNotice() {
    Card(
        backgroundColor = AppTheme.colors.background,
        shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.smallSpacing)
        ) {
            SimpleText(
                text = stringResource(R.string.common_important),
                style = ComposeTypographies.Paragraph2,
                color = ComposeColors.Warning,
                gravity = ComposeGravities.Start
            )

            TinyVerticalSpacer()

            SimpleText(
                text = stringResource(R.string.earn_active_rewards_withdrawal_blocked),
                style = ComposeTypographies.Caption1, color = ComposeColors.Title,
                gravity = ComposeGravities.Start
            )
        }
    }
}

@Preview
@Composable
fun PreviewActiveRewardsWithdrawalNotice() {
    AppTheme {
        ActiveRewardsWithdrawalNotice()
    }
}
