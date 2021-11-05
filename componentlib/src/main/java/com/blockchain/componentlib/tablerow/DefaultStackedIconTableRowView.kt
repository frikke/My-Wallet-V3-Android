package com.blockchain.componentlib.tablerow

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

class DefaultStackedIconTableRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    var primaryText by mutableStateOf("")
    var secondaryText by mutableStateOf("")
    var onClick by mutableStateOf({})
    var iconTopUrl by mutableStateOf("")
    var iconBottomUrl by mutableStateOf("")

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                DefaultStackedIconTableRow(
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    onClick = onClick,
                    iconTopUrl = iconTopUrl,
                    iconBottomUrl = iconBottomUrl,
                )
            }
        }
    }
}
