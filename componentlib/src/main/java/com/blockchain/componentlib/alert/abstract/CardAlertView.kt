package com.blockchain.componentlib.alert.abstract

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import com.blockchain.componentlib.alert.AlertType
import com.blockchain.componentlib.alert.CardAlert

class CardAlertView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    var title by mutableStateOf("")
    var subtitle by mutableStateOf("")
    var isBordered by mutableStateOf(false)
    var alertType by mutableStateOf(AlertType.Default)
    var onClose by mutableStateOf({})

    @Composable
    override fun Content() {
        CardAlert(
            title = title,
            subtitle = subtitle,
            alertType = alertType,
            isBordered = isBordered,
            onClose = onClose
        )
    }
}
