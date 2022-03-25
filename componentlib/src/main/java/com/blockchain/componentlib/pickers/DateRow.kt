package com.blockchain.componentlib.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark700
import com.blockchain.componentlib.theme.Grey100
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.theme.Grey900

@Composable
fun DateRow(
    dateRowData: DateRowData,
    isDarkMode: Boolean = isSystemInDarkTheme()
) {
    val textColor = if (isDarkMode) {
        Grey400
    } else {
        Grey900
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = dateRowData.onClick)
            .background(
                color = getDateRowBackgroundColor(dateRowData.isActive, isDarkMode),
                shape = AppTheme.shapes.small
            )
            .padding(
                horizontal = dimensionResource(R.dimen.medium_margin),
                vertical = dimensionResource(R.dimen.very_small_margin)
            )
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.CenterVertically),
            text = dateRowData.label,
            style = AppTheme.typography.body2,
            color = textColor
        )

        Spacer(modifier = Modifier.weight(1f, true))

        Text(
            modifier = Modifier
                .align(Alignment.CenterVertically),
            text = dateRowData.date,
            style = AppTheme.typography.caption2,
            color = AppTheme.colors.primary
        )

        Image(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(start = 28.dp, end = 22.dp)
                .rotate(
                    if (dateRowData.isActive) 180f else 0f
                ),
            imageResource = ImageResource.Local(R.drawable.ic_triangle_down, null)
        )
    }
}

@Composable
internal fun getDateRowBackgroundColor(isActive: Boolean, isDarkMode: Boolean): Color {
    return if (isActive) {
        if (isDarkMode) {
            Dark700
        } else {
            Grey100
        }
    } else {
        AppTheme.colors.light
    }
}

@Composable
@Preview
fun DateRow_Preview() {
    AppTheme {
        AppSurface {
            DateRow(DateRowData(label = "Date", date = "Sep 21, 2021"))
        }
    }
}
