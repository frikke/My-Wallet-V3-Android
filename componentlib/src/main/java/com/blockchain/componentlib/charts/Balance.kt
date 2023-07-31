package com.blockchain.componentlib.charts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun Balance(
    modifier: Modifier = Modifier,
    price: String,
    percentageChangeData: PercentageChangeData
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = price,
            style = AppTheme.typography.title1,
            color = AppTheme.colors.title
        )

        Spacer(modifier = Modifier.size(AppTheme.dimensions.tinySpacing))

        PercentageChange(
            priceChange = percentageChangeData.priceChange,
            valueChange = percentageChangeData.valueChange,
            interval = percentageChangeData.interval
        )
    }
}

@Preview
@Composable
private fun DefaultBalance_Preview() {
    AppTheme {
        AppSurface {
            Balance(
                price = "$2574.37",
                percentageChangeData = PercentageChangeData(
                    priceChange = "$50.00",
                    valueChange = ValueChange.fromValue(0.24),
                    interval = "Past Hour"
                )
            )
        }
    }
}
