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

class MarkdownTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var markdownText by mutableStateOf("")
    var style by mutableStateOf(ComposeTypographies.Body1)
    var color by mutableStateOf(ComposeColors.Light)
    var gravity by mutableStateOf(ComposeGravities.Start)

    @Composable
    override fun Content() {
        AppTheme(setSystemColors = false) {
            AppSurface {
                MarkdownText(
                    markdownText = markdownText,
                    style = style,
                    color = color,
                    gravity = gravity,
                )
            }
        }
    }
}
