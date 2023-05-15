package com.blockchain.componentlib.charts

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue400
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.componentlib.theme.Green400
import com.blockchain.componentlib.theme.Green700
import com.blockchain.componentlib.theme.Grey600
import com.blockchain.componentlib.theme.Pink700
import com.blockchain.componentlib.theme.Red400
import java.text.NumberFormat

@Composable
fun PercentageChange(
    modifier: Modifier = Modifier,
    priceChange: String,
    percentChange: Double,
    state: PercentageChangeState = PercentageChangeState.Neutral,
    interval: String,
    isDarkMode: Boolean = isSystemInDarkTheme()
) {
    val positiveTextColor = if (!isDarkMode) {
        Green700
    } else {
        Green400
    }

    val negativeTextColor = if (!isDarkMode) {
        Pink700
    } else {
        Red400
    }

    val neutralTextColor = if (!isDarkMode) {
        Blue600
    } else {
        Blue400
    }

    val priceColor = when (state) {
        PercentageChangeState.Positive -> positiveTextColor
        PercentageChangeState.Negative -> negativeTextColor
        PercentageChangeState.Neutral -> neutralTextColor
    }

    val intervalTextColor = if (!isDarkMode) {
        Grey600
    } else {
        Color.White
    }

    val arrow = when (state) {
        PercentageChangeState.Positive -> "↑"
        PercentageChangeState.Negative -> "↓"
        PercentageChangeState.Neutral -> "→"
    }

    val percentFormat = NumberFormat.getPercentInstance()
    percentFormat.maximumFractionDigits = 2
    val percentage = percentFormat.format(percentChange)

    Row(
        modifier = modifier
    ) {
        Text(
            text = arrow,
            style = AppTheme.typography.paragraph2,
            color = priceColor
        )

        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = priceChange,
            style = AppTheme.typography.paragraph2,
            color = priceColor
        )

        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = "($percentage)",
            style = AppTheme.typography.paragraph2,
            color = priceColor
        )

        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = interval,
            style = AppTheme.typography.paragraph2,
            color = intervalTextColor
        )
    }
}

data class PercentageChangeData(val priceChange: String, val percentChange: Double, val interval: String)

enum class PercentageChangeState {
    Positive, Negative, Neutral
}

@Preview
@Composable
fun DefaultPercentageChange_Preview() {
    AppTheme {
        AppSurface {
            PercentageChange(
                priceChange = "$50.00",
                percentChange = 0.24,
                state = PercentageChangeState.Neutral,
                interval = "Past Hour"
            )
        }
    }
}
