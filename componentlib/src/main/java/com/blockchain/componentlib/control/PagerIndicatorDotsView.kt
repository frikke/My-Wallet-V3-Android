package com.blockchain.componentlib.control

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class PagerIndicatorDotsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var selectedIndex by mutableStateOf(0)
    var count by mutableStateOf(2)

    @Composable
    override fun Content() {
        AppTheme(setSystemColors = false) {
            AppSurface {
                PagerIndicatorDots(
                    selectedIndex = selectedIndex,
                    count = count,
                )
            }
        }
    }

    fun clearState() {
        selectedIndex = 0
        count = 2
    }
}
