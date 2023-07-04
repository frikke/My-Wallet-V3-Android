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

class RadioView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var state by mutableStateOf(RadioButtonState.Unselected)
    var radioButtonEnabled by mutableStateOf(true)
    var onSelectedChanged: ((Boolean) -> Unit)? by mutableStateOf(null)

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                Radio(
                    state = state,
                    enabled = radioButtonEnabled,
                    onSelectedChanged = onSelectedChanged
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
