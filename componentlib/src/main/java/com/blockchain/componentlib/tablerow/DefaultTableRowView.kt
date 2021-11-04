package com.blockchain.componentlib.tablerow

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import com.blockchain.componentlib.tag.TagViewState

class DefaultTableRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    var primaryText by mutableStateOf("")
    var secondaryText by mutableStateOf(null as? String?)
    var paragraphText by mutableStateOf(null as? String?)
    var onClick by mutableStateOf({})
    var tags by mutableStateOf(null as? List<TagViewState>?)

    @Composable
    override fun Content() {
        DefaultTableRow(
            primaryText = primaryText,
            secondaryText = secondaryText,
            paragraphText = paragraphText,
            onClick = onClick,
            tags = tags
        )
    }
}
