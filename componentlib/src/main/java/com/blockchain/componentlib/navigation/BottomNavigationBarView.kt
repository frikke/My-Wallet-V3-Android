package com.blockchain.componentlib.navigation

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

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
    var isPulseAnimationEnabled by mutableStateOf(false)

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                BottomNavigationBar(
                    navigationItems,
                    onNavigationItemClick,
                    onMiddleButtonClick,
                    selectedNavigationItem,
                    bottomNavigationState,
                    isPulseAnimationEnabled
                )
            }
        }
    }

    fun clearState() {
        navigationItems = listOf(
            NavigationItem.Home,
            NavigationItem.Prices,
            NavigationItem.BuyAndSell,
            NavigationItem.Activity
        )
        onNavigationItemClick = { _: NavigationItem -> }
        onMiddleButtonClick = {}
        selectedNavigationItem = null
        bottomNavigationState = BottomNavigationState.Add
        isPulseAnimationEnabled = false
    }
}
