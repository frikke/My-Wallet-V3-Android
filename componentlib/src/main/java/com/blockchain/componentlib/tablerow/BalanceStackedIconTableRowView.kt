package com.blockchain.componentlib.tablerow

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.text.buildAnnotatedString
import com.blockchain.componentlib.tag.TagViewState
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

class BalanceStackedIconTableRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    var iconTopUrl by mutableStateOf("")
    var iconBottomUrl by mutableStateOf("")
    var titleStart by mutableStateOf(buildAnnotatedString { })
    var titleEnd by mutableStateOf(buildAnnotatedString { })
    var bodyStart by mutableStateOf(buildAnnotatedString { })
    var bodyEnd by mutableStateOf(buildAnnotatedString { })
    var onClick by mutableStateOf({})
    var tags by mutableStateOf(null as? List<TagViewState>?)

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                BalanceStackedIconTableRow(
                    titleStart = titleStart,
                    titleEnd = titleEnd,
                    bodyStart = bodyStart,
                    bodyEnd = bodyEnd,
                    onClick = onClick,
                    iconTopUrl = iconTopUrl,
                    iconBottomUrl = iconBottomUrl,
                )
            }
        }
    }
}
