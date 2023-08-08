package com.blockchain.componentlib.basic

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    var gravity by mutableStateOf(ComposeGravities.Start)
    var onClick by mutableStateOf(null as? (() -> Unit)?)
    var isMultiline by mutableStateOf(true)

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                SimpleText(
                    text = text,
                    modifier = Modifier
                        .wrapContentHeight()
                        .wrapContentWidth()
                        .let {
                            val onClick = onClick
                            if (onClick != null) {
                                it.clickable { onClick() }
                            } else {
                                it
                            }
                        },
                    style = style,
                    color = textColor,
                    gravity = gravity,
                    isMultiline = isMultiline
                )
            }
        }
    }
}
