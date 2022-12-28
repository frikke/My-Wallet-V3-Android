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
import com.google.android.material.snackbar.ContentViewCallback

class SnackbarAlertView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr), ContentViewCallback {

    var message by mutableStateOf("")
    var actionLabel by mutableStateOf("")
    var onClick by mutableStateOf({})
    var type by mutableStateOf(SnackbarType.Info)

    @Composable
    override fun Content() {
        AppTheme(setSystemColors = false) {
            AppSurface {
                SnackbarAlert(
                    message = message,
                    actionLabel = actionLabel,
                    onActionClicked = onClick,
                    type = type
                )
            }
        }
    }

    override fun animateContentIn(delay: Int, duration: Int) {
        // do nothing - here we can animate specific items when the snackbar comes in
    }

    override fun animateContentOut(delay: Int, duration: Int) {
        // do nothing - here we can animate specific items when the snackbar exits
    }
}

enum class SnackbarType {
    Success,
    Error,
    Warning,
    Info
}
