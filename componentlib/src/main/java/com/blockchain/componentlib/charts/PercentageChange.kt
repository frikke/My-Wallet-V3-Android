package com.blockchain.componentlib.charts

import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.tablerow.ValueChange
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun PercentageChange(
    modifier: Modifier = Modifier,
    priceChange: String,
    valueChange: ValueChange,
    interval: String,
) {
    Row(
        modifier = modifier
    ) {
        Text(
            text = valueChange.indicator,
            style = AppTheme.typography.paragraph2,
            color = valueChange.color
        )

        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = priceChange,
            style = AppTheme.typography.paragraph2,
            color = valueChange.color
        )

        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = "(${valueChange.value}%)",
            style = AppTheme.typography.paragraph2,
            color = valueChange.color
        )

        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = interval,
            style = AppTheme.typography.paragraph2,
            color = AppColors.body
        )
    }
}

data class PercentageChangeData(val priceChange: String, val valueChange: ValueChange, val interval: String)

@Preview
@Composable
fun DefaultPercentageChange_Preview() {
    PercentageChange(
        priceChange = "$50.00",
        valueChange = ValueChange.Down(12.30),
        interval = "Past Hour"
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DefaultPercentageChange_PreviewDark() {
    DefaultPercentageChange_Preview()
}
