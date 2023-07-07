package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.res.stringResource
import com.blockchain.componentlib.icons.Alert
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.lazylist.paddedItem
import com.blockchain.componentlib.lazylist.paddedRoundedCornersItems
import com.blockchain.componentlib.tablerow.TableRowHeader
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.activity.common.ActivityComponentItem
import com.blockchain.home.presentation.activity.common.ClickAction
import com.blockchain.home.presentation.activity.list.ActivityViewState
import com.blockchain.home.presentation.activity.list.TransactionGroup
import com.blockchain.walletmode.WalletMode

fun LazyListScope.homeActivityScreen(
    activityState: ActivityViewState,
    openActivity: () -> Unit,
    openActivityDetail: (String, WalletMode) -> Unit,
    wMode: WalletMode,
    showWarning: Boolean = false,
    warningOnClick: () -> Unit = {}
) {
    (activityState.activity as? DataResource.Data)?.data?.get(TransactionGroup.Combined)?.takeIf { activity ->
        activity.isNotEmpty()
    }?.let { activities ->
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
                title = stringResource(com.blockchain.stringResources.R.string.ma_home_activity_title),
                icon = Icons.Filled.Alert.withTint(AppColors.dark).takeIf { showWarning },
                iconOnClick = warningOnClick,
                actionTitle = stringResource(com.blockchain.stringResources.R.string.see_all),
                actionOnClick = {
                    openActivity()
                }
            )
        }

        paddedRoundedCornersItems(
            items = activities,
            key = { it.id },
            paddingValues = {
                PaddingValues(
                    start = AppTheme.dimensions.smallSpacing,
                    end = AppTheme.dimensions.smallSpacing,
                    bottom = AppTheme.dimensions.smallSpacing
                )
            }
        ) {
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
