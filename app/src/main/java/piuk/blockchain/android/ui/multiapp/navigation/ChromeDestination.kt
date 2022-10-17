package piuk.blockchain.android.ui.multiapp.navigation

import com.blockchain.commonarch.presentation.mvi_v2.compose.ComposeNavigationDestination

sealed class ChromeDestination(override val route: String) : ComposeNavigationDestination {
    object Main : ChromeDestination("Dashboard")
}
