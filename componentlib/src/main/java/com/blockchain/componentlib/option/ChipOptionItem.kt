package com.blockchain.componentlib.option

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.chip.ChipState
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey100
import com.blockchain.componentlib.theme.Grey800
import com.blockchain.componentlib.theme.Grey900

@Composable
internal fun ChipOptionItem(
    text: String,
    onClick: () -> Unit,
    state: ChipState = ChipState.Enabled
) {
    val defaultBackgroundColor = Color.White
    val selectedBackgroundColor = Grey800

    val disabledBackgroundColor = Grey100
    val defaultTextColor = AppTheme.colors.primary
    val selectedTextColor = Color.White
    val disabledTextColor = Grey900
    val defaultStrokeColor = Grey100

    val backgroundColor by animateColorAsState(
        when (state) {
            ChipState.Enabled -> defaultBackgroundColor
            ChipState.Selected -> selectedBackgroundColor
            ChipState.Disabled -> disabledBackgroundColor
        }
    )

    val textColor by animateColorAsState(
        when (state) {
            ChipState.Enabled -> defaultTextColor
            ChipState.Selected -> selectedTextColor
            ChipState.Disabled -> disabledTextColor
        }
    )
    val strokeColor by animateColorAsState(
        when (state) {
            ChipState.Enabled -> defaultStrokeColor
            ChipState.Selected -> Color.Transparent
            ChipState.Disabled -> Color.Transparent
        }
    )

    Row(
        Modifier
            .clip(AppTheme.shapes.extraLarge)
            .clickable {
                onClick()
            }
            .animateContentSize()
            .background(backgroundColor, AppTheme.shapes.extraLarge)
            .border(1.dp, strokeColor, AppTheme.shapes.extraLarge)
            .padding(
                horizontal = dimensionResource(com.blockchain.componentlib.R.dimen.very_small_spacing),
                vertical = dimensionResource(com.blockchain.componentlib.R.dimen.smallest_spacing)
            )
    ) {
        Text(
            text = text,
            modifier = Modifier.align(alignment = Alignment.CenterVertically),
            style = AppTheme.typography.paragraph1,
            color = textColor
        )
    }
}

@Preview
@Composable
fun DefaultChip_Basic() {
    AppTheme {
        AppSurface {
            ChipOptionItem(
                text = "Default1",
                state = ChipState.Selected,
                onClick = {}
            )
        }
    }
}
