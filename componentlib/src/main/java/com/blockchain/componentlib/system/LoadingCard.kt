package com.blockchain.componentlib.system

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun ShimmerLoadingCard(
    modifier: Modifier = Modifier,
    itemCount: Int = 2,
    backgroundColor: Color = AppTheme.colors.backgroundSecondary,
    shape: Shape = RoundedCornerShape(AppTheme.dimensions.mediumSpacing),
    elevation: Dp = 0.dp,
    showEndBlocks: Boolean = true,
    reversed: Boolean = false
) {
    Card(
        modifier = modifier,
        backgroundColor = backgroundColor,
        shape = shape,
        elevation = elevation
    ) {
        Column {
            (0 until itemCount).forEach {
                ShimmerLoadingTableRow(
                    showEndBlocks = showEndBlocks,
                    reversed = reversed
                )

                if (it < itemCount - 1) Divider(color = AppTheme.colors.background)
            }
        }
    }
}

@Preview
@Composable
private fun PreviewShimmerLoadingCard() {
    ShimmerLoadingCard()
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewShimmerLoadingCardDark() {
    PreviewShimmerLoadingCard()
}
