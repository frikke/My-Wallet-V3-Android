package com.blockchain.componentlib.basic

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class SimpleImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var image by mutableStateOf(ImageResource.None as ImageResource)
    var scaleType by mutableStateOf(ContentScale.Fit)
    var onClick by mutableStateOf(null as? (() -> Unit)?)
    var squared by mutableStateOf(false)

    @Composable
    override fun Content() {
        AppTheme(setSystemColors = false) {
            AppSurface {
                Image(
                    imageResource = image,
                    modifier = Modifier
                        .clickable(
                            enabled = onClick != null,
                            onClick = {
                                onClick?.let {
                                    it()
                                }
                            }
                        )
                        .then(
                            if (squared) {
                                Modifier.aspectRatio(1F)
                            } else {
                                Modifier
                            }
                        ),
                    contentScale = scaleType
                )
            }
        }
    }
}
