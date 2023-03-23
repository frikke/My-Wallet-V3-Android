package com.dex.presentation.graph

import androidx.navigation.NavGraphBuilder
import com.blockchain.chrome.composable.ChromeBottomSheet
import com.blockchain.chrome.composable.ChromeSingleScreen
import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationDestination
import com.blockchain.commonarch.presentation.mvi_v2.compose.bottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.compose.composable
import com.dex.presentation.DexIntroductionScreens
import com.dex.presentation.SelectDestinationAccountBottomSheet
import com.dex.presentation.SelectSourceAccountBottomSheet
import com.dex.presentation.SettingsBottomSheet
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi

@OptIn(ExperimentalMaterialNavigationApi::class)
fun NavGraphBuilder.dexGraph(onBackPressed: () -> Unit) {
    composable(navigationEvent = DexDestination.Intro) {
        ChromeSingleScreen {
            DexIntroductionScreens(onBackPressed)
        }
    }

    bottomSheet(navigationEvent = DexDestination.SelectSourceAccount) {
        ChromeBottomSheet(onBackPressed) {
            SelectSourceAccountBottomSheet(
                closeClicked = onBackPressed
            )
        }
    }

    bottomSheet(navigationEvent = DexDestination.SelectDestinationAccount) {
        ChromeBottomSheet(onBackPressed) {
            SelectDestinationAccountBottomSheet(
                closeClicked = onBackPressed
            )
        }
    }

    bottomSheet(navigationEvent = DexDestination.Settings) {
        SettingsBottomSheet(
            closeClicked = onBackPressed
        )
    }
}

sealed class DexDestination(
    override val route: String
) : ComposeNavigationDestination {
    object Intro : DexDestination("Intro")
    object SelectSourceAccount : DexDestination("SelectSourceAccount")
    object SelectDestinationAccount : DexDestination("SelectDestinationAccount")
    object Settings : DexDestination("Settings")
}
