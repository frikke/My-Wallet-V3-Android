package com.blockchain.componentlib.option

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.chip.ChipState
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun ChipOptionsGroup(
    modifier: Modifier = Modifier,
    options: List<ChipOption>,
) {
    if (options.isEmpty()) return
    var selectedOption by remember {
        mutableStateOf(
            options.firstOrNull { it.initialState == ChipState.Selected }
        )
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = AppTheme.dimensions.smallSpacing
            ),
        horizontalArrangement = Arrangement.spacedBy(
            AppTheme.dimensions.tinySpacing,
            Alignment.CenterHorizontally
        )
    ) {
        options.forEach { option ->
            ChipOptionItem(
                text = option.text,
                state = when {
                    option.initialState == ChipState.Disabled -> ChipState.Disabled
                    selectedOption == option -> ChipState.Selected
                    else -> ChipState.Enabled
                },
                onClick = {
                    if (option.initialState != ChipState.Disabled) {
                        selectedOption = option
                    }
                    option.onClick()
                }
            )
        }
    }
}

/**
 * we need to ignore onClick
 */
data class ChipOption(
    val text: String,
    val initialState: ChipState,
    val onClick: () -> Unit
)

@Preview
@Composable
fun ChipOptionsGroupPreview() {
    AppTheme {
        AppSurface {
            ChipOptionsGroup(
                options = listOf(
                    ChipOption(
                        text = "Option 1",
                        initialState = ChipState.Enabled,
                        onClick = {}
                    ),
                    ChipOption(
                        text = "Option 2",
                        initialState = ChipState.Enabled,
                        onClick = {}
                    ),
                    ChipOption(
                        text = "Option 3",
                        initialState = ChipState.Selected,
                        onClick = {}
                    )
                )
            )
        }
    }
}
