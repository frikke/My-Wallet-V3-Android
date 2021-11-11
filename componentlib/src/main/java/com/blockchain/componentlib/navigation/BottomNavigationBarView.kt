package com.blockchain.componentlib.navigation

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView

class BottomNavigationBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    var navigationItems by mutableStateOf(
        listOf(
            NavigationItem.Home,
            NavigationItem.Prices,
            NavigationItem.BuyAndSell,
            NavigationItem.Activity
        )
    )
    var onNavigationItemClick by mutableStateOf({ _: NavigationItem -> })
    var onMiddleButtonClick by mutableStateOf({})
    var selectedNavigationItem by mutableStateOf(null as? NavigationItem?)
    var bottomNavigationState by mutableStateOf(BottomNavigationState.Add)

    @Composable
    override fun Content() {
        BottomNavigationBar(
            navigationItems,
            onNavigationItemClick,
            onMiddleButtonClick,
            selectedNavigationItem,
            bottomNavigationState
        )
    }
}
