package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.basic.SimpleText
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.StandardVerticalSpacer
import com.blockchain.componentlib.utils.previewAnalytics
import com.blockchain.home.presentation.dashboard.DashboardAnalyticsEvents
import org.koin.androidx.compose.get

@Composable
fun NonCustodialEmptyStateCard(
    analytics: Analytics = get(),
    onReceiveClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(vertical = AppTheme.dimensions.smallSpacing)
            .fillMaxWidth()
    ) {
        Card(
            backgroundColor = AppTheme.colors.backgroundSecondary,
            shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
            elevation = 3.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(
                        horizontal = AppTheme.dimensions.smallSpacing
                    )
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StandardVerticalSpacer()

                Image(
                    imageResource = ImageResource.Local(
                        id = com.blockchain.componentlib.R.drawable.ic_empty_state_deposit,
                        contentDescription = stringResource(
                            id = com.blockchain.stringResources.R.string.nc_empty_state_title
                        )
                    )
                )

                StandardVerticalSpacer()

                SimpleText(
                    text = stringResource(id = com.blockchain.stringResources.R.string.nc_empty_state_title),
                    style = ComposeTypographies.Title3,
                    color = ComposeColors.Title,
                    gravity = ComposeGravities.Centre
                )

                SimpleText(
                    text = stringResource(
                        id = com.blockchain.stringResources.R.string.nc_empty_state_description
                    ),
                    style = ComposeTypographies.Body1,
                    color = ComposeColors.Body,
                    gravity = ComposeGravities.Centre,
                    modifier = Modifier.padding(
                        vertical = AppTheme.dimensions.smallSpacing
                    )
                )

                PrimaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = AppTheme.dimensions.standardSpacing,
                            horizontal = AppTheme.dimensions.smallSpacing
                        ),
                    text = stringResource(id = com.blockchain.stringResources.R.string.nc_empty_state_cta),
                    onClick = {
                        onReceiveClicked()
                        analytics.logEvent(DashboardAnalyticsEvents.EmptyStateReceiveCrypto)
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NonCustodialEmptyStateCardPreview() {
    NonCustodialEmptyStateCard(
        analytics = previewAnalytics,
        onReceiveClicked = {}
    )
}
