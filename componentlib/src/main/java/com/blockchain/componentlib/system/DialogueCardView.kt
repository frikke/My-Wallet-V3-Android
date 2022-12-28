package com.blockchain.componentlib.system

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

class DialogueCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var icon by mutableStateOf(ResourcesCompat.ID_NULL)
    var title by mutableStateOf(null as? String?)
    var body by mutableStateOf("")
    var firstButton by mutableStateOf(DialogueButton("", {}))
    var secondButton by mutableStateOf(null as? DialogueButton?)

    @Composable
    override fun Content() {
        AppTheme(setSystemColors = false) {
            AppSurface {
                DialogueCard(
                    icon = icon,
                    title = title,
                    body = body,
                    firstButton = firstButton,
                    secondButton = secondButton,
                )
            }
        }
    }

    fun clearState() {
        icon = ResourcesCompat.ID_NULL
        title = null
        body = ""
        firstButton = DialogueButton("", {})
        secondButton = null
    }
}
