package com.dex.presentation.graph

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.blockchain.chrome.composable.ChromeBottomSheet
import com.blockchain.chrome.composable.ChromeSingleScreen
import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationDestination
import com.blockchain.commonarch.presentation.mvi_v2.compose.bottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.compose.composable
import com.blockchain.commonarch.presentation.mvi_v2.compose.getComposeArgument
import com.blockchain.commonarch.presentation.mvi_v2.compose.wrappedArg
import com.dex.presentation.enteramount.AllowanceTxUiData
import com.dex.presentation.DexIntroductionScreens
import com.dex.presentation.SelectDestinationAccountBottomSheet
import com.dex.presentation.SelectSourceAccountBottomSheet
import com.dex.presentation.SettingsBottomSheet
import com.dex.presentation.TokenAllowanceBottomSheet
import com.dex.presentation.confirmation.DexConfirmationScreen
import com.dex.presentation.inprogress.DexInProgressTransactionScreen
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterialNavigationApi::class)
fun NavGraphBuilder.dexGraph(onBackPressed: () -> Unit, navController: NavController) {
    composable(navigationEvent = DexDestination.Intro) {
        ChromeSingleScreen {
            DexIntroductionScreens(onBackPressed)
        }
    }

    composable(navigationEvent = DexDestination.Confirmation) {
        ChromeSingleScreen {
            DexConfirmationScreen(onBackPressed, navController = navController)
        }
    }

    composable(navigationEvent = DexDestination.InProgress) {
        ChromeSingleScreen {
            DexInProgressTransactionScreen(onBackPressed = onBackPressed)
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

    bottomSheet(navigationEvent = DexDestination.TokenAllowanceSheet) {
        val data = it.arguments?.getComposeArgument(ARG_ALLOWANCE_TX) ?: throw IllegalArgumentException(
            "You must provide details"
        )
        val allowanceTxUiData = Json.decodeFromString(AllowanceTxUiData.serializer(), data)

        TokenAllowanceBottomSheet(
            closeClicked = onBackPressed,
            allowanceTxUiData = allowanceTxUiData,
            savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
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
    object Confirmation : DexDestination("Confirmation")
    object InProgress : DexDestination("InProgress")
    object TokenAllowanceSheet : DexDestination(route = "TokenAllowanceSheet/${ARG_ALLOWANCE_TX.wrappedArg()}}")
}

const val ARG_ALLOWANCE_TX = "ARG_ALLOWANCE_TX"
