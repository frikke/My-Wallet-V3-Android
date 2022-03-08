package com.blockchain.componentlib.alert

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class CardAlertView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var title by mutableStateOf("")
    var subtitle by mutableStateOf("")
    var isBordered by mutableStateOf(false)
    var alertType by mutableStateOf(AlertType.Default)
    var isDismissable by mutableStateOf(true)
    var onClose by mutableStateOf({})

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                CardAlert(
                    title = title,
                    subtitle = subtitle,
                    alertType = alertType,
                    isBordered = isBordered,
                    onClose = onClose,
                    isDismissable = isDismissable
                )
            }
        }
    }

    fun clearState() {
        title = ""
        subtitle = ""
        isBordered = false
        alertType = AlertType.Default
        onClose = {}
    }
}
