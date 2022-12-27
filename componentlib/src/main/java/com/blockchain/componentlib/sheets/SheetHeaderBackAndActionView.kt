package com.blockchain.componentlib.sheets

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class SheetHeaderBackAndActionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var title by mutableStateOf("")
    var actionType: SheetHeaderActionType by mutableStateOf(SheetHeaderActionType.Cancel("Cancel"))
    var onBackPress by mutableStateOf({ })
    var onActionPress by mutableStateOf({ })
    var backPressContentDescription by mutableStateOf(null as? String?)

    @Composable
    override fun Content() {
        AppTheme(setSystemColors = false) {
            AppSurface {
                SheetHeaderBackAndAction(
                    title = title,
                    onBackPress = onBackPress,
                    actionType = actionType,
                    onActionPress = onActionPress,
                    backPressContentDescription = backPressContentDescription,
                )
            }
        }
    }
}
