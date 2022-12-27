package com.blockchain.componentlib.system

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class LinearProgressBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var progress by mutableStateOf(null as? Float?)

    @Composable
    override fun Content() {
        AppTheme(setSystemColors = false) {
            AppSurface {
                LinearProgressBar(
                    progress = progress
                )
            }
        }
    }

    fun clearState() {
        progress = null
    }
}
