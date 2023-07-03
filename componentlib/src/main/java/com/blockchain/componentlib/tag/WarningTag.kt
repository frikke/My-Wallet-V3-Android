package com.blockchain.componentlib.tag

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.button.common.BaseButtonView
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class WarningTagView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var onClick : (() -> Unit)? by mutableStateOf(null)
    var text by mutableStateOf("")

    @Composable
    override fun Content() {
        if (isInEditMode) {
            text = "dummy tag text"
        }

        WarningTag(
            text = text,
            onClick = onClick
        )
    }
}


private val bgColorLight = Color(0XFFFFECD6)
private val bgColorDark = Color(0XFFFFA133)
private val bgColor @Composable get() = if (isSystemInDarkTheme()) bgColorDark else bgColorLight

private val textColorLight = Color(0XFFD46A00)
private val textColorDark = Color(0XFF07080D)
private val textColor @Composable get() = if (isSystemInDarkTheme()) textColorDark else textColorLight

@Composable
fun WarningTag(
    text: String,
    size: TagSize = TagSize.Primary,
    onClick: (() -> Unit)? = null
) {
    Tag(
        text = text,
        size = size,
        defaultBackgroundColor = bgColor,
        defaultTextColor = textColor,
        onClick = onClick
    )
}

@Preview
@Composable
fun WarningTag_Basic() {
    WarningTag(text = "Default", onClick = null)
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WarningTag_BasicDark() {
    WarningTag_Basic()
}

@Preview
@Composable
fun WarningTag_clickable() {
    WarningTag(text = "Click me", onClick = { })
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WarningTag_clickableDark() {
    WarningTag_clickable()
}
