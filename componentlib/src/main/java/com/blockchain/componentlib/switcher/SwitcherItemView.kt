package com.blockchain.componentlib.switcher

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ImageResource
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
    var endIcon: ImageResource.Local? by mutableStateOf(
        ImageResource.Local(
            contentDescription = "IconArrowRight",
            id = R.drawable.ic_arrow_right
        )
    )
    var switcherState by mutableStateOf(SwitcherState.Enabled)
    var indicator : SwitcherItemIndicator? by mutableStateOf(null)
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
                    indicator = indicator,
                    onClick = onClick,
                )
            }
        }
    }
}
