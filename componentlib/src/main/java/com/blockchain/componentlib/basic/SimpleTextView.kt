package com.blockchain.componentlib.basic

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class SimpleTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var text by mutableStateOf("")
    var style by mutableStateOf(ComposeTypographies.Body1)
    var textColor by mutableStateOf(ComposeColors.Medium)

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                SimpleText(
                    text = text,
                    style = style,
                    color = textColor
                )
            }
        }
    }
}
