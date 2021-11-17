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
    var title by mutableStateOf("")
    var navigationBarButtons by mutableStateOf(listOf<NavigationBarButton>())

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                NavigationBar(
                    title = title,
                    onBackButtonClick = onBackButtonClick,
                    navigationBarButtons = navigationBarButtons
                )
            }
        }
    }
}
