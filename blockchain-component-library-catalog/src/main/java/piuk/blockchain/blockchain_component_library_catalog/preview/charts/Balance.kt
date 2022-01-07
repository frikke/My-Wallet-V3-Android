package piuk.blockchain.blockchain_component_library_catalog.preview.charts

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.charts.Balance
import com.blockchain.componentlib.charts.PercentageChangeData
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "Default Balance", group = "Balance")
@Composable
fun DefaultBalance_Preview() {
    AppTheme {
        AppSurface {
            Balance(
                title = "Current Balance",
                price = "$2574.37",
                percentageChangeData = PercentageChangeData(
                    priceChange = "$50.00",
                    percentChange = 0.24,
                    interval = "Past Hour"
                )
            )
        }
    }
}