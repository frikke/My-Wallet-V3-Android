package com.blockchain.componentlib.divider

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

class VerticalDividerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                VerticalDivider(
                    modifier = Modifier.fillMaxHeight()
                )
            }
        }
    }
}
