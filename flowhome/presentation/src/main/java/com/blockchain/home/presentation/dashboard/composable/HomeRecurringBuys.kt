package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.R
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Sync
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.componentlib.lazylist.paddedRoundedCornersItems
import com.blockchain.componentlib.tablerow.ButtonTableRow
import com.blockchain.componentlib.tablerow.TableRowHeader
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.value
import com.blockchain.home.presentation.recurringbuy.RecurringBuysAnalyticsEvents
import com.blockchain.home.presentation.recurringbuy.list.RecurringBuyViewState
import com.blockchain.home.presentation.recurringbuy.list.composable.RecurringBuyTableRow
import org.koin.androidx.compose.get

internal fun LazyListScope.homeRecurringBuys(
    analytics: Analytics,
    recurringBuys: List<RecurringBuyViewState>,
    manageOnclick: () -> Unit,
    upsellOnClick: () -> Unit,
    recurringBuyOnClick: (String) -> Unit
) {
    paddedItem(
        paddingValues = PaddingValues(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.large_spacing)))
        TableRowHeader(
            title = stringResource(com.blockchain.stringResources.R.string.recurring_buy_toolbar),
            actionTitle = stringResource(
                com.blockchain.stringResources.R.string.manage
            ).takeIf { recurringBuys.isNotEmpty() },
            actionOnClick = {
                manageOnclick()
                analytics.logEvent(RecurringBuysAnalyticsEvents.ManageClicked)
            }.takeIf { recurringBuys.isNotEmpty() }
        )
        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
    }

    if (recurringBuys.isEmpty()) {
        paddedItem(
            paddingValues = PaddingValues(horizontal = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(AppTheme.dimensions.borderRadiiMedium)
            ) {
                ButtonTableRow(
                    title = stringResource(com.blockchain.stringResources.R.string.recurring_buy_automate_title),
                    subtitle = stringResource(
                        com.blockchain.stringResources.R.string.recurring_buy_automate_description
                    ),
                    imageResource = Icons.Filled.Sync.withTint(AppTheme.colors.primary),
                    actionText = stringResource(com.blockchain.stringResources.R.string.common_go),
                    onClick = {
                        upsellOnClick()
                        analytics.logEvent(RecurringBuysAnalyticsEvents.HomeCtaClicked)
                    }
                )
            }
        }
    } else {
        paddedRoundedCornersItems(
            items = recurringBuys,
            paddingValues = PaddingValues(horizontal = 16.dp)
        ) { recurringBuy ->
            RecurringBuyTableRow(
                description = recurringBuy.description.value(),
                status = recurringBuy.status.value(),
                iconUrl = recurringBuy.iconUrl,
                onClick = {
                    recurringBuyOnClick(recurringBuy.id)
                    analytics.logEvent(RecurringBuysAnalyticsEvents.HomeDetailClicked(recurringBuy.assetTicker))
                }
            )
        }
    }
}
