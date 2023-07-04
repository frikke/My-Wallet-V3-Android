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

class TabLayoutLargeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var items: List<String> by mutableStateOf(emptyList())
    var onItemSelected by mutableStateOf({ _: Int -> })
    var selectedItemIndex by mutableStateOf(0)
    var showBottomShadow by mutableStateOf(false)

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                TabLayoutLarge(
                    items = items,
                    onItemSelected = { index ->
                        selectedItemIndex = index
                        onItemSelected(index)
                    },
                    selectedItemIndex = selectedItemIndex,
                    hasBottomShadow = showBottomShadow
                )
            }
        }
    }

    fun clearState() {
        items = emptyList()
        onItemSelected = { _: Int -> }
        selectedItemIndex = 0
        showBottomShadow = false
    }
}
