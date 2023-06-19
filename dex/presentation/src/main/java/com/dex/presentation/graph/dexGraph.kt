package com.dex.presentation.graph

import androidx.activity.compose.BackHandler
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.blockchain.chrome.composable.ChromeBottomSheet
import com.blockchain.chrome.composable.ChromeSingleScreen
import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationDestination
import com.blockchain.commonarch.presentation.mvi_v2.compose.bottomSheet
import com.blockchain.commonarch.presentation.mvi_v2.compose.composable
import com.blockchain.commonarch.presentation.mvi_v2.compose.getComposeArgument
import com.blockchain.commonarch.presentation.mvi_v2.compose.wrappedArg
import com.dex.presentation.DexIntroductionScreens
import com.dex.presentation.NoNetworkFundsBottomSheet
import com.dex.presentation.SelectDestinationAccountBottomSheet
import com.dex.presentation.SelectSourceAccountBottomSheet
import com.dex.presentation.SettingsBottomSheet
import com.dex.presentation.TokenAllowanceBottomSheet
import com.dex.presentation.confirmation.DexConfirmationInfoSheet
import com.dex.presentation.confirmation.DexConfirmationScreen
import com.dex.presentation.enteramount.AllowanceTxUiData
import com.dex.presentation.inprogress.DexInProgressTransactionScreen
import com.dex.presentation.network.SelectNetwork
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import java.util.Base64
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterialNavigationApi::class)
fun NavGraphBuilder.dexGraph(onBackPressed: () -> Unit, navController: NavController) {
    composable(navigationEvent = DexDestination.Intro) {
        ChromeSingleScreen {
            DexIntroductionScreens(close = onBackPressed)
        }
    }

    composable(navigationEvent = DexDestination.Confirmation) {
        ChromeSingleScreen {
            DexConfirmationScreen(onBackPressed, navController = navController)
        }
    }

    composable(navigationEvent = DexDestination.InProgress) {
        BackHandler(true) {
        }
        ChromeSingleScreen {
            DexInProgressTransactionScreen(
                closeFlow = {
                    navController.popBackStack(
                        navController.graph.startDestinationId,
                        inclusive = false
                    )
                },
                retry = {
                    navController.popBackStack(DexDestination.Confirmation.route, false)
                }
            )
        }
    }

    bottomSheet(navigationEvent = DexDestination.SelectSourceAccount) {
        ChromeBottomSheet(fillMaxHeight = true, onClose = onBackPressed) {
            SelectSourceAccountBottomSheet(
                closeClicked = onBackPressed
            )
        }
    }

    bottomSheet(navigationEvent = DexDestination.DexConfirmationExtraInfoSheet) {
        val title = it.arguments?.getComposeArgument(ARG_INFO_TITLE) ?: throw IllegalArgumentException(
            "You must provide title"
        )
        val description = String(Base64.getUrlDecoder().decode(it.arguments?.getComposeArgument(ARG_INFO_DESCRIPTION)))
        ChromeBottomSheet(onClose = onBackPressed) {
            DexConfirmationInfoSheet(
                closeClicked = onBackPressed,
                title = title,
                description = description
            )
        }
    }

    bottomSheet(navigationEvent = DexDestination.SelectDestinationAccount) {
        ChromeBottomSheet(fillMaxHeight = true, onClose = onBackPressed) {
            SelectDestinationAccountBottomSheet(
                closeClicked = onBackPressed
            )
        }
    }

    bottomSheet(navigationEvent = DexDestination.Settings) {
        ChromeBottomSheet(onClose = onBackPressed) {
            SettingsBottomSheet(
                closeClicked = onBackPressed
            )
        }
    }

    bottomSheet(navigationEvent = DexDestination.SelectNetwork) {
        ChromeBottomSheet(onClose = onBackPressed) {
            SelectNetwork(
                closeClicked = onBackPressed
            )
        }
    }

    bottomSheet(navigationEvent = DexDestination.TokenAllowanceSheet) {
        val data = it.arguments?.getComposeArgument(ARG_ALLOWANCE_TX) ?: throw IllegalArgumentException(
            "You must provide details"
        )
        val allowanceTxUiData = Json.decodeFromString(AllowanceTxUiData.serializer(), data)

        ChromeBottomSheet(onClose = onBackPressed) {
            TokenAllowanceBottomSheet(
                closeClicked = onBackPressed,
                allowanceTxUiData = allowanceTxUiData,
                savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
            )
        }
    }
    bottomSheet(navigationEvent = DexDestination.NoFundsForSourceAccount) {
        val data = it.arguments?.getComposeArgument(ARG_CURRENCY_TICKER) ?: throw IllegalArgumentException(
            "You must provide details"
        )

        ChromeBottomSheet(onClose = onBackPressed) {
            NoNetworkFundsBottomSheet(
                closeClicked = onBackPressed,
                assetTicker = data,
                savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
            )
        }
    }
}

sealed class DexDestination(
    override val route: String
) : ComposeNavigationDestination {
    object Intro : DexDestination("Intro")
    object SelectSourceAccount : DexDestination("SelectSourceAccount")
    object NoFundsForSourceAccount : DexDestination(
        route = "NoFundsForSourceAccountSheet/${ARG_CURRENCY_TICKER.wrappedArg()}}"
    )

    object SelectDestinationAccount : DexDestination("SelectDestinationAccount")
    object Settings : DexDestination("Settings")
    object SelectNetwork : DexDestination("SelectNetwork")
    object Confirmation : DexDestination("Confirmation")
    object InProgress : DexDestination("InProgress")
    object TokenAllowanceSheet : DexDestination(route = "TokenAllowanceSheet/${ARG_ALLOWANCE_TX.wrappedArg()}}")
    object DexConfirmationExtraInfoSheet : DexDestination(
        route = "DexConfirmationExtraInfoSheet/${ARG_INFO_TITLE.wrappedArg()}/${ARG_INFO_DESCRIPTION.wrappedArg()}}"
    )
}

const val ARG_ALLOWANCE_TX = "ARG_ALLOWANCE_TX"
const val ARG_INFO_TITLE = "ARG_INFO_TITLE"
const val ARG_CURRENCY_TICKER = "ARG_CURRENCY_TICKER"
const val ARG_INFO_DESCRIPTION = "ARG_INFO_DESCRIPTION"
