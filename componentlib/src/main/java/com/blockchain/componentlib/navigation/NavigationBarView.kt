package com.blockchain.componentlib.navigation

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    var applyModeColors by mutableStateOf(true)
    var mutedBackground by mutableStateOf(true)

    @Composable
    override fun Content() {
        if (startNavigationButton != null) {
            if (applyModeColors) {
                NavigationBar(
                    mutedBg = mutedBackground,
                    title = title,
                    startNavigationBarButton = startNavigationButton,
                    endNavigationBarButtons = endNavigationBarButtons
                )
            } else {
                NavigationBar(
                    walletMode = null,
                    mutedBg = mutedBackground,
                    title = title,
                    startNavigationBarButton = startNavigationButton,
                    endNavigationBarButtons = endNavigationBarButtons
                )
            }
        } else {
            if (applyModeColors) {
                NavigationBar(
                    mutedBg = mutedBackground,
                    title = title,
                    onBackButtonClick = onBackButtonClick,
                    navigationBarButtons = endNavigationBarButtons
                )
            } else {
                NavigationBar(
                    walletMode = null,
                    mutedBg = mutedBackground,
                    title = title,
                    onBackButtonClick = onBackButtonClick,
                    navigationBarButtons = endNavigationBarButtons
                )
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
