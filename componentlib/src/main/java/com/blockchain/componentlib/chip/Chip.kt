package com.blockchain.componentlib.chip

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Blue400
import com.blockchain.componentlib.theme.Blue600
import com.blockchain.componentlib.theme.Dark200
import com.blockchain.componentlib.theme.Dark800
import com.blockchain.componentlib.theme.Grey000
import com.blockchain.componentlib.theme.Grey100
import com.blockchain.componentlib.theme.Grey600
import com.blockchain.componentlib.theme.Grey900

@Composable
fun Chip(
    text: String,
    onClick: (ChipState) -> Unit,
    initialChipState: ChipState = ChipState.Enabled
) {
    var chipState by remember { mutableStateOf(initialChipState) }
    val size by animateDpAsState(if (chipState != ChipState.Selected) 0.dp else 18.dp)

    val defaultBackgroundColor = if (!isSystemInDarkTheme()) {
        Grey000
    } else {
        Dark800
    }

    val selectedBackgroundColor = if (!isSystemInDarkTheme()) {
        Blue600
    } else {
        Blue400
    }

    val disabledBackgroundColor = if (!isSystemInDarkTheme()) {
        Grey100
    } else {
        Dark800
    }

    val defaultTextColor = if (!isSystemInDarkTheme()) {
        Grey900
    } else {
        Color.White
    }

    val selectedTextColor = if (!isSystemInDarkTheme()) {
        Color.White
    } else {
        Grey600
    }

    val disabledTextColor = if (!isSystemInDarkTheme()) {
        Grey900
    } else {
        Dark200
    }

    val backgroundColor by animateColorAsState(
        when (chipState) {
            ChipState.Enabled -> defaultBackgroundColor
            ChipState.Selected -> selectedBackgroundColor
            ChipState.Disabled -> disabledBackgroundColor
        }
    )

    val textColor by animateColorAsState(
        when (chipState) {
            ChipState.Enabled -> defaultTextColor
            ChipState.Selected -> selectedTextColor
            ChipState.Disabled -> disabledTextColor
        }
    )

    Row(
        Modifier
            .clip(AppTheme.shapes.extraLarge)
            .clickable {
                chipState = when (chipState) {
                    ChipState.Enabled -> ChipState.Selected
                    ChipState.Selected -> ChipState.Enabled
                    ChipState.Disabled -> ChipState.Disabled
                }
                onClick(chipState)
            }
            .animateContentSize()
            .background(backgroundColor, AppTheme.shapes.extraLarge)
            .padding(
                horizontal = dimensionResource(com.blockchain.componentlib.R.dimen.very_small_spacing),
                vertical = dimensionResource(com.blockchain.componentlib.R.dimen.smallest_spacing)
            )
    ) {
        Image(
            modifier = Modifier
                .align(alignment = Alignment.CenterVertically)
                .height(size)
                .width(size)
                .padding(end = dimensionResource(com.blockchain.componentlib.R.dimen.minuscule_spacing)),
            painter = painterResource(id = R.drawable.ic_chip_checkmark),
            contentDescription = null,
            colorFilter = ColorFilter.tint(textColor)
        )

        Text(
            text = text,
            modifier = Modifier.align(alignment = Alignment.CenterVertically),
            style = AppTheme.typography.paragraph1,
            color = textColor
        )
    }
}

enum class ChipState {
    Enabled, Selected, Disabled
}

@Preview
@Composable
fun DefaultChip_Basic() {
    AppTheme {
        AppSurface {
            Chip(
                text = "Default",
                initialChipState = ChipState.Selected,
                onClick = {}
            )
        }
    }
}

@Preview
@Composable
fun DefaultChip_Basic_Selected() {
    AppTheme {
        AppSurface {
            Chip(text = "Default", onClick = {})
        }
    }
}

@Preview
@Composable
fun DefaultChip_Dark() {
    AppTheme(darkTheme = true) {
        AppSurface {
            Chip(text = "Default", onClick = {})
        }
    }
}
