package com.blockchain.home.presentation.dashboard.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.blockchain.coincore.AssetAction
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.domain.onboarding.CompletableDashboardOnboardingStep
import com.blockchain.domain.onboarding.DashboardOnboardingStep
import com.blockchain.domain.onboarding.DashboardOnboardingStepState
import com.blockchain.home.presentation.navigation.AssetActionsNavigation
import com.blockchain.home.presentation.quickactions.QuickActions
import com.blockchain.koin.payloadScope
import org.koin.androidx.compose.getViewModel

@Composable
fun HomeScreen(
    listState: LazyListState,
    assetActionsNavigation: AssetActionsNavigation,
    openCryptoAssets: () -> Unit,
    openActivity: () -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color(0XFFF1F2F7),
                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ),
    ) {
        item {
            Balance()
        }

        item {
            QuickActions(assetActionsNavigation = assetActionsNavigation)
        }
        item {
            EmptyCard(
                onboardingLaunch = {
                    assetActionsNavigation.onBoardingNavigation(
                        DashboardOnboardingStep.values().map { step ->
                            CompletableDashboardOnboardingStep(
                                step = step,
                                state = DashboardOnboardingStepState.INCOMPLETE
                            )
                        }
                    )
                },
                onReceive = { assetActionsNavigation.navigate(AssetAction.Receive) },
                onBuy = { assetActionsNavigation.navigate(AssetAction.Buy) },
                homeAssetsViewModel = getViewModel(scope = payloadScope),
                pkwActivityViewModel = getViewModel(scope = payloadScope),
                custodialActivityViewModel = getViewModel(scope = payloadScope)
            )
        }
        item {
            HomeAssets(
                openAllAssets = openCryptoAssets
            )
        }

        item {
            HomeActivity(
                openAllActivity = openActivity
            )
        }

        item {
            HelpAndSupport()
        }

        item {
            Spacer(modifier = Modifier.size(AppTheme.dimensions.borderRadiiLarge))
        }
    }
}
