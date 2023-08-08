package com.blockchain.transactions.receive.navigation

import androidx.navigation.NavGraphBuilder
import com.blockchain.chrome.composable.ChromeBottomSheet
import com.blockchain.chrome.composable.ChromeSingleScreen
import com.blockchain.chrome.navigation.LocalAssetActionsNavigationProvider
import com.blockchain.commonarch.presentation.mvi_v2.compose.bottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.compose.composable
import com.blockchain.presentation.customviews.kyc.KycUpgradeNowScreen
import com.blockchain.transactions.receive.accounts.composable.ReceiveAccounts
import com.blockchain.transactions.receive.detail.composable.ReceiveAccountDetail
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi

@OptIn(ExperimentalMaterialNavigationApi::class)
fun NavGraphBuilder.receiveGraph(
    onBackPressed: () -> Unit
) {
    composable(navigationEvent = ReceiveDestination.Accounts) {
        ChromeSingleScreen {
            ReceiveAccounts(
                onBackPressed = onBackPressed
            )
        }
    }

    bottomSheet(navigationEvent = ReceiveDestination.AccountDetail) {
        ChromeBottomSheet(fillMaxHeight = true, onClose = onBackPressed) {
            ReceiveAccountDetail(
                onBackPressed = onBackPressed
            )
        }
    }

    bottomSheet(navigationEvent = ReceiveDestination.KycUpgrade) {
        val assetNavigation = LocalAssetActionsNavigationProvider.current

        ChromeBottomSheet(fillMaxHeight = true, onClose = onBackPressed) {
            KycUpgradeNowScreen(
                isBottomSheet = true,
                startKycClicked = {
                    onBackPressed()
                    assetNavigation.startKyc()
                }
            )
        }
    }
}
