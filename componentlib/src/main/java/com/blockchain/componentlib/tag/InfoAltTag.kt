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
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class InfoAltTagView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var text by mutableStateOf("")

    @Composable
    override fun Content() {
        if (isInEditMode) {
            text = "dummy text"
        }
        InfoAltTag(
            text = text,
        )
    }
}

private val bgColorLight = Color(0XFFECF5FE)
private val bgColorDark = Color(0XFF3B3E46)
private val bgColor @Composable get() = if (isSystemInDarkTheme()) bgColorDark else bgColorLight

private val textColorLight = Color(0XFF0C6CF2)
private val textColorDark = Color(0XFF65A5FF)
private val textColor @Composable get() = if (isSystemInDarkTheme()) textColorDark else textColorLight

@Composable
fun InfoAltTag(
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
private fun InfoAltTag_Basic() {
    InfoAltTag(text = "Default", onClick = null)
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InfoAltTag_BasicDark() {
    InfoAltTag_Basic()
}

@Preview
@Composable
fun InfoAltTag_clickable() {
    InfoAltTag(text = "Click me", onClick = { })
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InfoAltTag_clickableDark() {
    InfoAltTag_clickable()
}
