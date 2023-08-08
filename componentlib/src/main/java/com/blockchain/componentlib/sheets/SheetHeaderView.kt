package com.blockchain.componentlib.sheets

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class SheetHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var title by mutableStateOf(null as? String?)
    var byline by mutableStateOf(null as? String?)
    var onClosePress by mutableStateOf({ })
    var startImageResource: ImageResource by mutableStateOf(ImageResource.None)

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                SheetHeader(
                    title = title,
                    byline = byline,
                    startImage = StackedIcon.SingleIcon(startImageResource),
                    onClosePress = onClosePress,
                    secondaryBackground = true
                )
            }
        }
    }
}
