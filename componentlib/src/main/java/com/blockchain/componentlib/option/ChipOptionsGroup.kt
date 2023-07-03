package com.blockchain.componentlib.option

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.button.MinimalPrimarySmallButton
import com.blockchain.componentlib.button.SecondarySmallButton
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

data class ChipOption(
    val id: Any,
    val text: String,
    val isActive: Boolean
)

@Composable
fun ChipOptionsGroup(
    modifier: Modifier = Modifier,
    options: List<ChipOption>,
    onClick: (ChipOption) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(
            AppTheme.dimensions.tinySpacing,
            Alignment.CenterHorizontally
        )
    ) {
        options.forEach { option ->
            when (option.isActive) {
                true -> {
                    SecondarySmallButton(
                        modifier = Modifier.weight(1F),
                        text = option.text,
                        onClick = { onClick(option) }
                    )
                }

                false -> {
                    MinimalPrimarySmallButton(
                        modifier = Modifier.weight(1F),
                        text = option.text,
                        onClick = { onClick(option) }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun ChipOptionsGroupPreview() {
    AppTheme {
        AppSurface {
            ChipOptionsGroup(
                options = listOf(
                    ChipOption(
                        id = "1",
                        text = "Option 1",
                        isActive = false
                    ),
                    ChipOption(
                        id = "2",
                        text = "Option 2",
                        isActive = true
                    ),
                    ChipOption(
                        id = "3",
                        text = "Option 3",
                        isActive = false

                    )
                ),
                onClick = {}
            )
        }
    }
}
