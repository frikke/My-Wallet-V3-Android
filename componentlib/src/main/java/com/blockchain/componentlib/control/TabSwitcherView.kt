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
import kotlinx.collections.immutable.toImmutableList

class TabSwitcherView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var tabs: List<String> by mutableStateOf(emptyList())
    var onTabChanged by mutableStateOf({ _: Int -> })
    var initialTabIndex by mutableStateOf(0)

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                TabSwitcher(
                    initialTabIndex = initialTabIndex,
                    tabs = tabs.toImmutableList(),
                    onTabChanged = onTabChanged
                )
            }
        }
    }

    fun clearState() {
        tabs = emptyList()
    }
}
