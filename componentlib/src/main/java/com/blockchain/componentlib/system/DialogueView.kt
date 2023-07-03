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

class DialogueView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var body by mutableStateOf("")
    var firstButton by mutableStateOf(DialogueButton("", {}))
    var secondButton by mutableStateOf(null as? DialogueButton?)

    @Composable
    override fun Content() {
        AppTheme {
            AppSurface {
                Dialogue(
                    body = body,
                    firstButton = firstButton,
                    secondButton = secondButton
                )
            }
        }
    }

    fun clearState() {
        body = ""
        firstButton = DialogueButton("", {})
        secondButton = null
    }
}
