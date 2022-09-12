package com.blockchain.componentlib.charts

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun Balance(
    title: String,
    price: String,
    percentageChangeData: PercentageChangeData,
    endIcon: ImageResource = ImageResource.None
) {
    Surface(color = AppTheme.colors.background) {
        Column(
            modifier = Modifier
                .padding(
                    horizontal = dimensionResource(R.dimen.standard_spacing),
                    vertical = dimensionResource(R.dimen.medium_spacing)
                )
                .fillMaxWidth()
        ) {
            Text(
                text = title,
                style = AppTheme.typography.caption2,
                color = AppTheme.colors.title
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = price,
                    style = AppTheme.typography.title1,
                    color = AppTheme.colors.title
                )

                Image(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(
                            top = dimensionResource(R.dimen.tiny_spacing),
                            start = dimensionResource(R.dimen.medium_spacing)
                        )
                        .size(dimensionResource(R.dimen.large_spacing)),
                    imageResource = endIcon
                )
            }

            PercentageChange(
                modifier = Modifier.padding(top = 8.dp),
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

@Preview
@Composable
fun DefaultBalance_Preview_With_Icon() {
    AppTheme {
        AppSurface {
            Balance(
                title = "Current Balance",
                price = "$2574.37",
                percentageChangeData = PercentageChangeData(
                    priceChange = "$50.00",
                    percentChange = 0.24,
                    interval = "Past Hour"
                ),
                endIcon = ImageResource.Local(R.drawable.ic_blockchain)
            )
        }
    }
}

@Preview
@Composable
fun DefaultBalance_Preview_With_Bitmap() {
    val width = 50
    val height = 50
    val bitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint()
    paint.color = Color.BLACK
    paint.style = Paint.Style.FILL
    canvas.drawPaint(paint)

    paint.color = Color.WHITE
    paint.isAntiAlias = true
    paint.textSize = 14f
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("Hello Android!", width / 2f, height / 2f, paint)

    AppTheme {
        AppSurface {
            Balance(
                title = "Current Balance",
                price = "$2574.37",
                percentageChangeData = PercentageChangeData(
                    priceChange = "$50.00",
                    percentChange = 0.24,
                    interval = "Past Hour"
                ),
                endIcon = ImageResource.LocalWithResolvedBitmap(bitmap)
            )
        }
    }
}
