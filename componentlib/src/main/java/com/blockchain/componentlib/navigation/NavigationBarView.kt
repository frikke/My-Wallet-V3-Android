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

class NavigationBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    var onBackButtonClick by mutableStateOf(null as? (() -> Unit)?)
    var startNavigationBarButton by mutableStateOf(null as NavigationBarButton.Icon?)
    var title by mutableStateOf("")
    var endNavigationBarButtons by mutableStateOf(listOf<NavigationBarButton>())

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                if (startNavigationBarButton != null) {
                    NavigationBar(
                        title = title,
                        startNavigationBarButton = startNavigationBarButton,
                        endNavigationBarButtons = endNavigationBarButtons
                    )
                } else {
                    NavigationBar(
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
        startNavigationBarButton = null
        title = ""
        endNavigationBarButtons = listOf()
    }
}
