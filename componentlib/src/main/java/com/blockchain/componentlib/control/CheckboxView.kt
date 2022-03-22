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

class CheckboxView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var onCheckChanged: ((Boolean) -> Unit)? by mutableStateOf(null)
    var state by mutableStateOf(CheckboxState.Unchecked)
    var checkboxEnabled by mutableStateOf(true)

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                Checkbox(
                    state = state,
                    enabled = checkboxEnabled,
                    onCheckChanged = onCheckChanged,
                )
            }
        }
    }

    fun clearState() {
        onCheckChanged = { _: Boolean -> }
        state = CheckboxState.Unchecked
        checkboxEnabled = true
    }
}
