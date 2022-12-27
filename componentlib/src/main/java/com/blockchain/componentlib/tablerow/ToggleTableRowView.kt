package com.blockchain.componentlib.tablerow

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class ToggleTableRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var primaryText by mutableStateOf("")
    var secondaryText by mutableStateOf("")
    var isChecked by mutableStateOf(false)
    var onCheckedChange by mutableStateOf({ isChecked: Boolean -> })
    var toggleEnabled by mutableStateOf(true)
    var toggleType by mutableStateOf(ToggleTableRowType.Primary)

    @Composable
    override fun Content() {
        AppTheme(setSystemColors = false) {
            AppSurface {
                ToggleTableRow(
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    isChecked = isChecked,
                    onCheckedChange = { newCheckedState ->
                        isChecked = newCheckedState
                        onCheckedChange(newCheckedState)
                    },
                    enabled = toggleEnabled,
                    toggleTableRowType = toggleType,
                )
            }
        }
    }

    fun clearState() {
        primaryText = ""
        secondaryText = ""
        isChecked = false
        onCheckedChange = { isChecked: Boolean -> }
        toggleEnabled = true
        toggleType = ToggleTableRowType.Primary
    }
}
