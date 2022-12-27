package com.blockchain.componentlib.system

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.blockchain.componentlib.charts.SparkLineHistoricalRate
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView
import kotlin.random.Random

class LoadingChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var loadingText by mutableStateOf("")
    var historicalRates: List<SparkLineHistoricalRate> by mutableStateOf(
        List(20) {
            object : SparkLineHistoricalRate {
                override val timestamp: Long = it.toLong()
                override val rate: Double = Random.nextDouble(50.0, 150.0)
            }
        }
    )

    @Composable
    override fun Content() {
        AppTheme(setSystemColors = false) {
            AppSurface {
                LoadingChart(
                    historicalRates = historicalRates,
                    loadingText = loadingText
                )
            }
        }
    }
}
