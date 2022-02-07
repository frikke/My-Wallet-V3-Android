package com.blockchain.componentlib.chip

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class ChipView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var text by mutableStateOf("")
    var onClick by mutableStateOf({ _: ChipState -> })
    var initialChipState by mutableStateOf(ChipState.Enabled)

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                Chip(
                    text = text,
                    onClick = onClick,
                    initialChipState = initialChipState
                )
            }
        }
    }

    fun clearState() {
        text = ""
        onClick = { _: ChipState -> }
        initialChipState = ChipState.Enabled
    }
}
