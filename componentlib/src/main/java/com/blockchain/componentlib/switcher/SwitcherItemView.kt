package com.blockchain.componentlib.switcher

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.ChevronRight
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class SwitcherItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var text by mutableStateOf("")
    var startIcon: ImageResource.Local? by mutableStateOf(null)
    var endIcon: ImageResource.Local? by mutableStateOf(Icons.ChevronRight)
    var switcherState by mutableStateOf(SwitcherState.Enabled)
    var showIndicator: Boolean by mutableStateOf(false)
    var onClick by mutableStateOf({})

    @Composable
    override fun Content() {
        AppTheme(setSystemColors = false) {
            AppSurface {
                SwitcherItem(
                    text = text,
                    state = switcherState,
                    startIcon = startIcon,
                    endIcon = endIcon,
                    showIndicator = showIndicator,
                    onClick = onClick
                )
            }
        }
    }
}
