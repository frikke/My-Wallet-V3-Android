package com.blockchain.componentlib.control

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

class RadioView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    var state by mutableStateOf(RadioButtonState.Unselected)
    var radioButtonEnabled by mutableStateOf(true)
    var onSelectedChanged by mutableStateOf({ _: Boolean -> })

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                Radio(
                    state = state,
                    enabled = radioButtonEnabled,
                    onSelectedChanged = { isSelected ->
                        state = if (isSelected) RadioButtonState.Selected else RadioButtonState.Unselected
                        onSelectedChanged(isSelected)
                    }
                )
            }
        }
    }

    fun clearState() {
        state = RadioButtonState.Unselected
        radioButtonEnabled = true
        onSelectedChanged = { _: Boolean -> }
    }
}
