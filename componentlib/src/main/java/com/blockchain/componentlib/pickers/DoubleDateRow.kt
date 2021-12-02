package com.blockchain.componentlib.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.image.Image
import com.blockchain.componentlib.image.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.theme.Grey900

@Composable
fun DoubleDateRow(
    topDateRowData: DateRowData,
    bottomDateRowData: DateRowData,
    isDarkMode: Boolean = isSystemInDarkTheme()
) {
    val textColor = if (isDarkMode) {
        Grey400
    } else {
        Grey900
    }

    val topRowShape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomEnd = 0.dp, bottomStart = 0.dp)
    val bottomRowShape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomEnd = 4.dp, bottomStart = 4.dp)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = topDateRowData.onClick)
                .background(
                    color = getDateRowBackgroundColor(isActive = topDateRowData.isActive, isDarkMode = isDarkMode),
                    shape = topRowShape
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                modifier = Modifier
                    .align(Alignment.CenterVertically),
                text = topDateRowData.label,
                style = AppTheme.typography.body2,
                color = textColor
            )

            Spacer(modifier = Modifier.weight(1f, true))

            Text(
                modifier = Modifier
                    .align(Alignment.CenterVertically),
                text = topDateRowData.date,
                style = AppTheme.typography.caption2,
                color = AppTheme.colors.primary
            )

            Image(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(start = 28.dp, end = 22.dp)
                    .rotate(
                        if (topDateRowData.isActive) 180f else 0f
                    ),
                imageResource = ImageResource.Local(R.drawable.ic_triangle_down, null)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = bottomDateRowData.onClick)
                .background(
                    color = getDateRowBackgroundColor(isActive = bottomDateRowData.isActive, isDarkMode = isDarkMode),
                    shape = bottomRowShape
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                modifier = Modifier
                    .align(Alignment.CenterVertically),
                text = bottomDateRowData.label,
                style = AppTheme.typography.body2,
                color = textColor
            )

            Spacer(modifier = Modifier.weight(1f, true))

            Text(
                modifier = Modifier
                    .align(Alignment.CenterVertically),
                text = bottomDateRowData.date,
                style = AppTheme.typography.caption2,
                color = AppTheme.colors.primary
            )

            Image(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(start = 28.dp, end = 22.dp)
                    .rotate(
                        if (bottomDateRowData.isActive) 180f else 0f
                    ),
                imageResource = ImageResource.Local(R.drawable.ic_triangle_down, null)
            )
        }
    }
}

@Composable
@Preview
fun DoubleDateRow_Preview() {
    AppTheme {
        AppSurface {
            DoubleDateRow(
                topDateRowData = DateRowData(label = "Begins", date = "Sep 21, 2021"),
                bottomDateRowData = DateRowData(label = "Ends", date = "Sep 21, 2021", isActive = true)
            )
        }
    }
}
