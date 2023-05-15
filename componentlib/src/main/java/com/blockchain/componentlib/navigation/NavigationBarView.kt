package com.blockchain.componentlib.navigation

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class NavigationBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var onBackButtonClick by mutableStateOf(null as? (() -> Unit)?)
    var startNavigationButton by mutableStateOf(null as? NavigationBarButton?)

    var icon: StackedIcon by mutableStateOf(StackedIcon.None)

    var title by mutableStateOf("")
    var endNavigationBarButtons by mutableStateOf(listOf<NavigationBarButton>())

    var modeColor: ModeBackgroundColor by mutableStateOf(ModeBackgroundColor.Current)
    var mutedBackground by mutableStateOf(false)

    @Composable
    override fun Content() {
        AppTheme(setSystemColors = false) {
            AppSurface {
                if (startNavigationButton != null) {
                    NavigationBar(
                        modeColor = modeColor,
                        mutedBackground = mutedBackground,
                        title = title,
                        icon = icon,
                        startNavigationBarButton = startNavigationButton,
                        endNavigationBarButtons = endNavigationBarButtons
                    )
                } else {
                    NavigationBar(
                        modeColor = modeColor,
                        mutedBackground = mutedBackground,
                        title = title,
                        icon = icon,
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
