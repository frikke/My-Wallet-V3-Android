package com.blockchain.componentlib.charts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
            percentChange = percentageChangeData.percentChange,
            interval = percentageChangeData.interval,
            state = when {
                percentageChangeData.percentChange < 0.0 -> {
                    PercentageChangeState.Negative
                }
                percentageChangeData.percentChange > 0.0 -> {
                    PercentageChangeState.Positive
                }
                else -> {
                    PercentageChangeState.Neutral
                }
            }
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
                    percentChange = 0.24,
                    interval = "Past Hour"
                )
            )
        }
    }
}
