package com.blockchain.componentlib.basic

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class SimpleImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var image by mutableStateOf(ImageResource.None as ImageResource)
    var imageSize by mutableStateOf(24)
    var scaleType by mutableStateOf(ContentScale.Fit)

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                Image(
                    imageResource = image,
                    modifier = Modifier.size(imageSize.dp),
                    contentScale = scaleType
                )
            }
        }
    }
}
