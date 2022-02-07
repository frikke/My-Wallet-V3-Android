package com.blockchain.componentlib.tablerow

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.buildAnnotatedString
import com.blockchain.componentlib.image.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class BalanceStackedIconTableRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var topImageResource: ImageResource by mutableStateOf(ImageResource.None)
    var bottomImageResource: ImageResource by mutableStateOf(ImageResource.None)
    var titleStart by mutableStateOf(buildAnnotatedString { })
    var titleEnd by mutableStateOf(buildAnnotatedString { })
    var bodyStart by mutableStateOf(buildAnnotatedString { })
    var bodyEnd by mutableStateOf(buildAnnotatedString { })
    var onClick by mutableStateOf({})

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
                    topImageResource = topImageResource,
                    bottomImageResource = bottomImageResource,
                )
            }
        }
    }

    fun clearState() {
        topImageResource = ImageResource.None
        bottomImageResource = ImageResource.None
        titleStart = buildAnnotatedString { }
        titleEnd = buildAnnotatedString { }
        bodyStart = buildAnnotatedString { }
        bodyEnd = buildAnnotatedString { }
        onClick = {}
    }
}
