package piuk.blockchain.android.ui.collapseheader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.blockchain.componentlib.theme.AppTheme
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
            onSettingsButtonClicked = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        )
}

@Preview
@Composable
fun CollapsingToolbarHalfwayPreview() {
        CollapsingToolbar(
            progress = 0.5f,
            onPrivacyTipButtonClicked = {},
            onSettingsButtonClicked = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )
}

@Preview
@Composable
fun CollapsingToolbarExpandedPreview() {
        CollapsingToolbar(
            progress = 1f,
            onPrivacyTipButtonClicked = {},
            onSettingsButtonClicked = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        )
}

@Composable
fun CollapsingToolbar(
    progress: Float,
    onPrivacyTipButtonClicked: () -> Unit,
    onSettingsButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colors.primary,
        elevation = Elevation,
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = ContentPadding)
                    .fillMaxSize()
            ) {
                CollapsingToolbarLayout(progress = progress) {
                    Text(
                        modifier = Modifier.padding(start = dimensionResource(R.dimen.tiny_margin)),
                        style = AppTheme.typography.title3,
                        color = Color.Black,
                        text = "total balance: xxxx"
                    )

                    Text(
                        modifier = Modifier.padding(start = dimensionResource(R.dimen.tiny_margin)),
                        style = AppTheme.typography.title3,
                        color = Color.Black,
                        text = "trading - defi"
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
                x = totalBalance.width / 2,
                y = lerp(
                    start = -totalBalance.height,
                    stop = expandedHorizontalGuideline - totalBalance.height,
                    fraction = progress
                )
            )
            pager.placeRelative(
                x = pager.width / 2,
                y = lerp(
                    start = collapsedHorizontalGuideline - pager.height / 2,
                    stop = expandedHorizontalGuideline,
                    fraction = progress
                )
            )
        }
    }
}
