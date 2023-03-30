package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.componentlib.lazylist.paddedRoundedCornersItems
import com.blockchain.componentlib.tablerow.TableRowHeader
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.value
import com.blockchain.home.presentation.recurringbuy.RecurringBuyViewState
import com.blockchain.home.presentation.recurringbuy.composable.RecurringBuyTableRow

internal fun LazyListScope.homeRecurringBuys(
    recurringBuys: List<RecurringBuyViewState>,
    manageOnclick: () -> Unit
) {
    paddedItem(
        paddingValues = PaddingValues(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.size(dimensionResource(R.dimen.large_spacing)))
        TableRowHeader(
            title = stringResource(R.string.recurring_buy_toolbar),
            actionTitle = stringResource(R.string.manage).takeIf { recurringBuys.isNotEmpty() },
            actionOnClick = manageOnclick.takeIf { recurringBuys.isNotEmpty() },
        )
        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
    }

    if (recurringBuys.isEmpty()) {
        // upsell
    } else {
        paddedRoundedCornersItems(
            items = recurringBuys,
            paddingValues = PaddingValues(horizontal = 16.dp)
        ) { recurringBuy ->
            RecurringBuyTableRow(
                description = recurringBuy.description.value(),
                status = recurringBuy.status.value(),
                iconUrl = recurringBuy.iconUrl,
                onClick = {}
            )
        }
    }
}
