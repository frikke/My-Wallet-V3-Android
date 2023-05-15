package com.blockchain.componentlib.tablerow

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.blockchain.componentlib.tablerow.custom.StackedIcon
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class BalanceFiatAndCryptoTableRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var title by mutableStateOf("")
    var subtitle by mutableStateOf("")
    var tag by mutableStateOf("")
    var valueCrypto by mutableStateOf("")
    var valueFiat by mutableStateOf("")
    var icon: StackedIcon by mutableStateOf(StackedIcon.None)
    var defaultIconSize by mutableStateOf(24.dp)
    var onClick by mutableStateOf({})

    var subView: View? by mutableStateOf(null)

    var roundedTop by mutableStateOf(false)
    var roundedBottom by mutableStateOf(false)
    var withBorder by mutableStateOf(false)

    @Composable
    private fun getRoundSize(rounded: Boolean) =
        if (rounded) {
            AppTheme.dimensions.borderRadiiMedium
        } else 0.dp

    @Composable
    override fun Content() {
        Surface(
            modifier = Modifier.clickable(onClick = onClick),
            shape = RoundedCornerShape(
                topStart = getRoundSize(roundedTop),
                topEnd = getRoundSize(roundedTop),
                bottomStart = getRoundSize(roundedBottom),
                bottomEnd = getRoundSize(roundedBottom)
            ),
            border = BorderStroke(1.dp, Blue600).takeIf { withBorder }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                BalanceFiatAndCryptoTableRow(
                    title = title,
                    subtitle = subtitle,
                    tag = tag,
                    valueCrypto = valueCrypto,
                    valueFiat = valueFiat,
                    icon = icon,
                    defaultIconSize = defaultIconSize,
                    onClick = onClick
                )

                subView?.let { subView ->
                    AndroidView(
                        modifier = Modifier.padding(
                            start = AppTheme.dimensions.smallSpacing,
                            end = AppTheme.dimensions.smallSpacing,
                            bottom = AppTheme.dimensions.smallSpacing
                        ),
                        factory = {
                            subView
                        }
                    )
                }
            }
        }
    }
}
