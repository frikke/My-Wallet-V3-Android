package piuk.blockchain.android.ui.collapseheader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.END_DEFI
import com.blockchain.componentlib.theme.END_TRADING
import com.blockchain.componentlib.theme.START_DEFI
import com.blockchain.componentlib.theme.START_TRADING
import com.blockchain.componentlib.utils.clickableNoEffect
import piuk.blockchain.android.R
import kotlin.math.roundToInt

private val ContentPadding = 8.dp
private val Elevation = 4.dp

@Preview
@Composable
fun CollapsingToolbarCollapsedPreview() {
    CollapsingToolbar(
        progress = 0f,
        onPrivacyTipButtonClicked = {},
        onTradingClicked = {},
        onDefiClicked = {},
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        START_TRADING,
                        END_TRADING
                    )
                )
            )
    )
}

@Preview
@Composable
fun CollapsingToolbarHalfwayPreview() {
    CollapsingToolbar(
        progress = 0.5f,
        onPrivacyTipButtonClicked = {},
        onTradingClicked = {},
        onDefiClicked = {},
        modifier = Modifier
            .fillMaxWidth()
            .height(81.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        START_DEFI,
                        END_DEFI
                    )
                )
            )
    )
}

@Preview
@Composable
fun CollapsingToolbarExpandedPreview() {
    CollapsingToolbar(
        progress = 1f,
        onPrivacyTipButtonClicked = {},
        onTradingClicked = {},
        onDefiClicked = {},
        modifier = Modifier
            .fillMaxWidth()
            .height(108.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        START_TRADING,
                        END_TRADING
                    )
                )
            )
    )
}

@Composable
fun CollapsingToolbar(
    progress: Float,
    onPrivacyTipButtonClicked: () -> Unit,
    onTradingClicked: () -> Unit,
    onDefiClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
        Box(modifier = modifier) {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = ContentPadding)
                    .fillMaxSize()
            ) {
                CollapsingToolbarLayout(progress = progress) {
                    Box(modifier = Modifier
                        .clickableNoEffect { onPrivacyTipButtonClicked() }
                        .height(54.dp)
                        .fillMaxWidth()
                        /*.background(Color.Red)*/) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(start = dimensionResource(R.dimen.tiny_margin)),
                            style = AppTheme.typography.title3,
                            color = Color.Black,
                            text = "total balance: xxxx"
                        )
                    }

                    Row(
                        modifier = Modifier
                            .height(54.dp)
                            .fillMaxWidth()
                            /*.background(Color.Green)*/,
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(start = dimensionResource(R.dimen.tiny_margin))
                                .clickableNoEffect { onTradingClicked() },
                            style = AppTheme.typography.title3,
                            color = Color.Black,
                            text = "trading"
                        )

                        Spacer(modifier = Modifier.size(32.dp))

                        Text(
                            modifier = Modifier
                                .padding(start = dimensionResource(R.dimen.tiny_margin))
                                .clickableNoEffect { onDefiClicked() },
                            style = AppTheme.typography.title3,
                            color = Color.Black,
                            text = "defi"
                        )
                    }
                }
            }
    }
}

@Composable
private fun CollapsingToolbarLayout(
    progress: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        check(measurables.size == 2)

        val placeables = measurables.map {
            it.measure(constraints)
        }
        layout(
            width = constraints.maxWidth,
            height = constraints.maxHeight
        ) {

            val expandedHorizontalGuideline = (constraints.maxHeight * 0.4f).roundToInt()
            val collapsedHorizontalGuideline = (constraints.maxHeight * 0.5f).roundToInt()

            val totalBalance = placeables[0]
            val pager = placeables[1]

            totalBalance.placeRelative(
                x = constraints.maxWidth / 2 - totalBalance.width / 2, // center
                y = lerp(
                    start = -totalBalance.height, // off the screen
                    stop = 0,
                    fraction = progress
                )
            )
            pager.placeRelative(
                x = constraints.maxWidth / 2 - pager.width / 2, // center
                y = constraints.maxHeight - pager.height, // bottom
            )
        }
    }
}
