package piuk.blockchain.blockchain_component_library_catalog.preview.charts

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.charts.PercentageChange
import com.blockchain.componentlib.charts.PercentageChangeState
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "Default PercentageChange", group = "PercentageChange")
@Composable
fun DefaultPercentageChange_Preview() {
    AppTheme {
        AppSurface {
            PercentageChange(
                priceChange = "$50.00",
                percentChange = 0.24,
                state = PercentageChangeState.Positive,
                interval = "Past Hour"
            )
        }
    }
}

@Preview(name = "Negative PercentageChange", group = "PercentageChange")
@Composable
fun NegativePercentageChange_Preview() {
    AppTheme {
        AppSurface {
            PercentageChange(
                priceChange = "$50.00",
                percentChange = -0.24,
                state = PercentageChangeState.Negative,
                interval = "Past Hour"
            )
        }
    }
}