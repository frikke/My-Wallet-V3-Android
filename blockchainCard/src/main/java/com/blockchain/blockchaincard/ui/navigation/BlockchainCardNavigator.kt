package com.blockchain.blockchaincard.ui.navigation

import androidx.navigation.NavOptionsBuilder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Compose Navigation handler
 */
class BlockchainCardNavigator {
    // Create shared flow that will pass on the next screen in the navigation and some optional navigation options
    private val _sharedFlow =
        MutableSharedFlow<Pair<NavTarget, NavOptionsBuilder.() -> Unit>>(extraBufferCapacity = 1)
    val sharedFlow = _sharedFlow.asSharedFlow()

    /**
     * Signal this flow's subscribers to navigate to navTarget using navOptions (optional)
     *
     * @param navTarget
     * @param navOptions
     */
    fun navigateTo(navTarget: NavTarget, navOptions: NavOptionsBuilder.() -> Unit = {}) {
        _sharedFlow.tryEmit(Pair(navTarget, navOptions))
    }

    // Possible navigation targets
    enum class NavTarget(val label: String) {
        OrderOrLinkCard("order_or_link_card"),
        SelectCardForOrder("select_card_for_order")
    }
}