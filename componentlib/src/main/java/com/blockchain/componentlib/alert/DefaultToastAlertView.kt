package com.blockchain.componentlib.alert

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class DefaultToastAlertView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var text by mutableStateOf("")
    var startIcon by mutableStateOf(ImageResource.None as ImageResource)
    var onClick by mutableStateOf({})

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                DefaultToastAlert(
                    text = text,
                    onClick = onClick,
                    startIcon = startIcon
                )
            }
        }
    }
}
