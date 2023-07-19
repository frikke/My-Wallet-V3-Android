package com.blockchain.componentlib.basic

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ColorFilter
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Info
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey300
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class SmallInfoWithIconView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var iconUrl: String? by mutableStateOf(null)
    var text: String by mutableStateOf("")

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                SmallInfoWithIcon(
                    iconUrl = iconUrl,
                    text = text,
                    trailingIcon = Icons.Filled.Info.copy(
                        colorFilter = ColorFilter.tint(AppColors.muted)
                    )
                )
            }
        }
    }
}
