package com.blockchain.componentlib.basic

import android.content.Context
import android.util.AttributeSet
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class AppDividerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    @Composable
    override fun Content() {
        AppDivider()
    }
}

@Composable
fun AppDivider(color: Color = AppColors.background) {
    Divider(color = color)
}
