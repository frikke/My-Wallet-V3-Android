package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.blockchain.coincore.AssetAction
import com.blockchain.componentlib.chrome.MenuOptionsScreen
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.collectAsStateLifecycleAware
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.allassets.AssetsViewModel
import com.blockchain.home.presentation.allassets.AssetsViewState
import com.blockchain.home.presentation.earn.EarnAssets
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import com.blockchain.home.presentation.navigation.SupportNavigation
import com.blockchain.home.presentation.quickactions.QuickActions
import com.blockchain.koin.payloadScope
import com.blockchain.walletmode.WalletMode
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    listState: LazyListState,
    shouldTriggerRefresh: Boolean,
    assetActionsNavigation: AssetActionsNavigation,
    supportNavigation: SupportNavigation,
    openSettings: () -> Unit,
    launchQrScanner: () -> Unit,
    openCryptoAssets: () -> Unit,
    openActivity: () -> Unit,
    openActivityDetail: (String, WalletMode) -> Unit,
    openReferral: () -> Unit,
    openFiatActionDetail: (String) -> Unit,
    openMoreQuickActions: () -> Unit,
) {
    var menuOptionsHeight: Int by remember { mutableStateOf(0) }
    var balanceOffsetToMenuOption: Float by remember { mutableStateOf(0F) }
    val balanceToMenuPaddingPx: Int = LocalDensity.current.run { 24.dp.toPx() }.toInt()
    var balanceScrollRange: Float by remember { mutableStateOf(0F) }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(
                AppTheme.colors.backgroundMuted,
                AppTheme.shapes.veryLarge
            ),
    ) {
        stickyHeader {

            val viewState: AssetsViewState by getViewModel<AssetsViewModel>(scope = payloadScope)
                .viewState
                .collectAsStateLifecycleAware()

            MenuOptionsScreen(
                modifier = Modifier.onGloballyPositioned {
                    menuOptionsHeight = it.size.height
                },
                walletBalance = (viewState.balance.balance as? DataResource.Data)?.data?.toStringWithSymbol() ?: "",
                openSettings = openSettings,
                launchQrScanner = launchQrScanner,
                showBackground = balanceOffsetToMenuOption <= 0F && menuOptionsHeight > 0F,
                showBalance = balanceScrollRange <= 0.5 && menuOptionsHeight > 0F
            )
        }

        item {
            Balance(
                modifier = Modifier.onGloballyPositioned {
                    (it.positionInParent().y - menuOptionsHeight + balanceToMenuPaddingPx)
                        .coerceAtLeast(0F).let {
                            if (balanceOffsetToMenuOption != it) balanceOffsetToMenuOption = it
                        }

                    ((it.positionInParent().y / menuOptionsHeight.toFloat()) * 2).coerceIn(0F, 1F).let {
                        if (balanceScrollRange != it) balanceScrollRange = it
                    }
                },
                scrollRange = balanceScrollRange,
                hideBalance = balanceScrollRange <= 0.5 && menuOptionsHeight > 0F
            )
        }

        item {
            QuickActions(
                assetActionsNavigation = assetActionsNavigation,
                openMoreQuickActions = openMoreQuickActions
            )
        }
        item {
            EmptyCard(
                onReceive = { assetActionsNavigation.navigate(AssetAction.Receive) },
                assetActionsNavigation = assetActionsNavigation,
                homeAssetsViewModel = getViewModel(scope = payloadScope),
                pkwActivityViewModel = getViewModel(scope = payloadScope),
                custodialActivityViewModel = getViewModel(scope = payloadScope)
            )
        }
        item {
            HomeAssets(
                assetActionsNavigation = assetActionsNavigation,
                forceRefresh = shouldTriggerRefresh,
                openAllAssets = openCryptoAssets,
                openFiatActionDetail = openFiatActionDetail
            )
        }

        item {
            EarnAssets(assetActionsNavigation = assetActionsNavigation)
        }

        item {
            HomeActivity(
                openAllActivity = openActivity,
                openActivityDetail = openActivityDetail
            )
        }

        item {
            Referral(
                openReferral = openReferral
            )
        }

        item {
            HelpAndSupport(
                openSupportChat = { supportNavigation.launchSupportChat() },
                openSupportCenter = { supportNavigation.launchSupportCenter() }
            )
        }

        item {
            Spacer(modifier = Modifier.size(AppTheme.dimensions.borderRadiiLarge))
        }
    }
}
