package com.blockchain.componentlib.navigation

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class NavigationBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var onBackButtonClick by mutableStateOf(null as? (() -> Unit)?)
    var startNavigationButton by mutableStateOf(null as? NavigationBarButton?)

    var title by mutableStateOf("")
    var endNavigationBarButtons by mutableStateOf(listOf<NavigationBarButton>())

    var ignoreWalletModeColor by mutableStateOf(false)
    var mutedBackground by mutableStateOf(false)

    @Composable
    override fun Content() {
        AppTheme(setSystemColors = false) {
            AppSurface {
                if (startNavigationButton != null) {
                    NavigationBar(
                        ignoreWalletModeColor = ignoreWalletModeColor,
                        mutedBackground = mutedBackground,
                        title = title,
                        startNavigationBarButton = startNavigationButton,
                        endNavigationBarButtons = endNavigationBarButtons
                    )
                } else {
                    NavigationBar(
                        ignoreWalletModeColor = ignoreWalletModeColor,
                        mutedBackground = mutedBackground,
                        title = title,
                        onBackButtonClick = onBackButtonClick,
                        navigationBarButtons = endNavigationBarButtons
                    )
                }
            }
        }
    }

    fun clearState() {
        onBackButtonClick = null
        startNavigationButton = null
        title = ""
        endNavigationBarButtons = listOf()
    }
}
