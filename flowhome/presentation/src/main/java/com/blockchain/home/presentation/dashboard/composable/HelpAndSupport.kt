package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.home.presentation.R
import com.blockchain.home.presentation.dashboard.DashboardAnalyticsEvents
import org.koin.androidx.compose.get

@Composable
fun HelpAndSupport(
    analytics: Analytics = get(),
    openSupportCenter: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.dimensions.smallSpacing)
    ) {

        SimpleText(
            text = stringResource(R.string.ma_home_need_help),
            style = ComposeTypographies.Body2,
            color = ComposeColors.Body,
            gravity = ComposeGravities.Start
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

        Card(
            backgroundColor = AppTheme.colors.background,
            shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
            elevation = 0.dp
        ) {
            DefaultTableRow(
                primaryText = stringResource(R.string.view_support_center),
                onClick = {
                    openSupportCenter()
                    analytics.logEvent(DashboardAnalyticsEvents.SupportClicked)
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HelpAndSupportPreview() {
    HelpAndSupport(
        openSupportCenter = {}
    )
}
