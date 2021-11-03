package com.blockchain.componentlib.tablerow

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView

class ActionStackedIconTableRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    var primaryText by mutableStateOf("")
    var secondaryText by mutableStateOf(null as? String?)
    var onClick by mutableStateOf({})
    var iconTopUrl by mutableStateOf("")
    var iconButtomUrl by mutableStateOf("")

    @Composable
    override fun Content() {
        ActionStackedIconTableRow(
            primaryText = primaryText,
            secondaryText = secondaryText,
            onClick = onClick,
            iconTopUrl = iconTopUrl,
            iconButtomUrl = iconButtomUrl,
        )
    }
}