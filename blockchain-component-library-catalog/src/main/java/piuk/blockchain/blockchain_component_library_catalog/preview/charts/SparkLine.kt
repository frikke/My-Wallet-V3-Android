package piuk.blockchain.blockchain_component_library_catalog.preview.charts

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.charts.SparkLine
import com.blockchain.componentlib.charts.SparkLineHistoricalRate
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview
@Composable
fun SparkLinePreview() {
    val data: List<SparkLineHistoricalRate> = List(20) {
        object : SparkLineHistoricalRate {
            override val timestamp: Long = it.toLong()
            override val rate: Double = Math.random() * 1000
        }
    }

    AppTheme {
        AppSurface {
            SparkLine(
                historicalRates = data,
                modifier = Modifier.size(64.dp, 16.dp)
            )
        }
    }
}