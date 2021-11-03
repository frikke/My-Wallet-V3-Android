package com.blockchain.componentlib.tablerow

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView

class ToggleTableRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    var title by mutableStateOf("")
    var isChecked by mutableStateOf(false)
    var onCheckedChange by mutableStateOf({ isChecked: Boolean -> })

    @Composable
    override fun Content() {
        ToggleTableRow(
            title = title,
            isChecked = isChecked,
            onCheckedChange = { newCheckedState ->
                isChecked = newCheckedState
                onCheckedChange(newCheckedState)
            }
        )
    }
}
