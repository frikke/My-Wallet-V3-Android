package com.blockchain.componentlib.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun Balance(
    title: String,
    price: String,
    percentageChangeData: PercentageChangeData
) {
    Surface(color = AppTheme.colors.background) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = title,
                style = AppTheme.typography.caption2,
                color = AppTheme.colors.title
            )

            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = price,
                style = AppTheme.typography.title1,
                color = AppTheme.colors.title
            )

            PercentageChange(
                modifier = Modifier.padding(top = 8.dp),
                priceChange = percentageChangeData.priceChange,
                percentChange = percentageChangeData.percentChange,
                interval = percentageChangeData.interval
            )
        }
    }
}

@Preview
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
