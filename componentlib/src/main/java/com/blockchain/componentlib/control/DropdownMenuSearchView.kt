package com.blockchain.componentlib.control

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import com.blockchain.componentlib.basic.ComposeColors
import com.blockchain.componentlib.basic.ComposeGravities
import com.blockchain.componentlib.basic.ComposeTypographies
import com.blockchain.componentlib.controls.TextInputState
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class DropdownMenuSearchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var value by mutableStateOf(TextFieldValue(""))
    var onValueChange by mutableStateOf({ _: TextFieldValue -> })
    var suggestions = mutableStateListOf<String>()
    var style by mutableStateOf(ComposeTypographies.Body1)
    var textColor by mutableStateOf(ComposeColors.Medium)
    var gravity by mutableStateOf(ComposeGravities.Start)
    var onClick by mutableStateOf(null as? (() -> Unit)?)
    var state: TextInputState by mutableStateOf(TextInputState.Default())

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                DropdownMenuSearch(
                    value = value,
                    onValueChange = onValueChange,
                    initialSuggestions = suggestions,
                )
            }
        }
    }
}
