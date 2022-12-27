package com.blockchain.componentlib.tablerow

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.buildAnnotatedString
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.charts.SparkLineHistoricalRate
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class BalanceTableRowLargeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var startImageResource: ImageResource by mutableStateOf(ImageResource.None)
    var title by mutableStateOf("")
    var primaryBylineStart by mutableStateOf(buildAnnotatedString { })
    var primaryBylineEnd by mutableStateOf(buildAnnotatedString { })
    var secondaryBylineStart by mutableStateOf(buildAnnotatedString { })
    var secondaryBylineEnd by mutableStateOf(buildAnnotatedString { })
    var onClick by mutableStateOf({})
    var historicalRates: List<SparkLineHistoricalRate> by mutableStateOf(emptyList())

    @Composable
    override fun Content() {
        AppTheme(setSystemColors = false) {
            AppSurface {
                BalanceTableRowLarge(
                    title = title,
                    historicalRates = historicalRates,
                    primaryBylineStart = primaryBylineStart,
                    primaryBylineEnd = primaryBylineEnd,
                    secondaryBylineStart = secondaryBylineStart,
                    secondaryBylineEnd = secondaryBylineEnd,
                    startImageResource = startImageResource,
                    onClick = onClick
                )
            }
        }
    }

    fun clearState() {
        startImageResource = ImageResource.None
        title = ""
        primaryBylineStart = buildAnnotatedString { }
        primaryBylineEnd = buildAnnotatedString { }
        secondaryBylineStart = buildAnnotatedString { }
        secondaryBylineEnd = buildAnnotatedString { }
        onClick = {}
        historicalRates = emptyList()
    }
}
