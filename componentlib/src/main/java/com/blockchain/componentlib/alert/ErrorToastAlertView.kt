package com.blockchain.componentlib.alert

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.res.ResourcesCompat
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class ErrorToastAlertView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var text by mutableStateOf("")
    var startIconDrawableRes by mutableStateOf(ResourcesCompat.ID_NULL)

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                ErrorToastAlert(
                    text = text
                )
            }
        }
    }

    fun clearState() {
        text = ""
        startIconDrawableRes = ResourcesCompat.ID_NULL
    }
}
