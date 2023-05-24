package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.componentlib.tablerow.DefaultTableRow
import com.blockchain.componentlib.tablerow.TableRowHeader
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.home.presentation.dashboard.DashboardAnalyticsEvents
import org.koin.androidx.compose.get

fun LazyListScope.homeHelp(
    openSupportCenter: () -> Unit
) {
    paddedItem(
        paddingValues = {
            PaddingValues(
                start = AppTheme.dimensions.smallSpacing,
                end = AppTheme.dimensions.smallSpacing,
                top = AppTheme.dimensions.smallSpacing,
                bottom = AppTheme.dimensions.tinySpacing
            )
        }
    ) {
        TableRowHeader(
            title = stringResource(com.blockchain.stringResources.R.string.ma_home_need_help)
        )
    }

    paddedItem(
        paddingValues = {
            PaddingValues(
                start = AppTheme.dimensions.smallSpacing,
                end = AppTheme.dimensions.smallSpacing,
                bottom = AppTheme.dimensions.smallSpacing
            )
        }
    ) {
        HelpAndSupport(
            openSupportCenter = openSupportCenter
        )
    }
}

@Composable
private fun HelpAndSupport(
    analytics: Analytics = get(),
    openSupportCenter: () -> Unit
) {
    Card(
        backgroundColor = AppTheme.colors.background,
        shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
        elevation = 0.dp
    ) {
        DefaultTableRow(
            primaryText = stringResource(com.blockchain.stringResources.R.string.view_support_center),
            onClick = {
                openSupportCenter()
                analytics.logEvent(DashboardAnalyticsEvents.SupportClicked)
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HelpAndSupportPreview() {
    HelpAndSupport(
        openSupportCenter = {}
    )
}
