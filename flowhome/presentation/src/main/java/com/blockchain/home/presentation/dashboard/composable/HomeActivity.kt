package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.blockchain.analytics.Analytics
import com.blockchain.componentlib.R
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.componentlib.lazylist.paddedRoundedCornersItems
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey700
import com.blockchain.componentlib.utils.clickableNoEffect
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.activity.common.ActivityComponentItem
import com.blockchain.home.presentation.activity.common.ClickAction
import com.blockchain.home.presentation.activity.list.ActivityViewState
import com.blockchain.home.presentation.activity.list.TransactionGroup
import com.blockchain.home.presentation.dashboard.DashboardAnalyticsEvents
import com.blockchain.walletmode.WalletMode
import org.koin.androidx.compose.get

@Composable
fun HomeActivityHeader(
    analytics: Analytics = get(),
    openActivity: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.ma_home_activity_title),
            style = AppTheme.typography.body2,
            color = Grey700
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            modifier = Modifier.clickableNoEffect {
                openActivity()
                analytics.logEvent(DashboardAnalyticsEvents.ActivitySeeAllClicked)
            },
            text = stringResource(R.string.see_all),
            style = AppTheme.typography.paragraph2,
            color = AppTheme.colors.primary,
        )
    }
}

fun LazyListScope.homeActivityScreen(
    activityState: ActivityViewState,
    openActivity: () -> Unit,
    openActivityDetail: (String, WalletMode) -> Unit,
    wMode: WalletMode
) {
    (activityState.activity as? DataResource.Data)?.data?.get(TransactionGroup.Combined)?.takeIf { activity ->
        activity.isNotEmpty()
    }?.let { activities ->
        paddedItem {
            Spacer(modifier = Modifier.size(AppTheme.dimensions.largeSpacing))
            HomeActivityHeader(openActivity = openActivity)
            Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))
        }
        paddedRoundedCornersItems(items = activities, key = { it.id }) {
            ActivityComponentItem(
                component = it,
                onClick = { clickAction ->
                    (clickAction as? ClickAction.Stack)?.data?.let { data ->
                        openActivityDetail(data, wMode)
                    }
                }
            )
        }
    }
}
