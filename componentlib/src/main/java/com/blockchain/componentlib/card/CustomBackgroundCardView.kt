package com.blockchain.componentlib.card

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class CustomBackgroundCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var title by mutableStateOf("")
    var subtitle by mutableStateOf("")
    var iconResource: ImageResource by mutableStateOf(ImageResource.None)
    var onClose by mutableStateOf({})
    var backgroundResource: ImageResource by mutableStateOf(ImageResource.None)
    var isCloseable: Boolean by mutableStateOf(true)
    var onClick: () -> Unit by mutableStateOf({})
    var textColor: @Composable () -> Color by mutableStateOf({ AppColors.title })

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                CustomBackgroundCard(
                    title = title,
                    subtitle = subtitle,
                    iconResource = iconResource,
                    onClose = onClose,
                    backgroundResource = backgroundResource,
                    isCloseable = isCloseable,
                    onClick = onClick,
                    textColor = textColor()
                )
            }
        }
    }
}
