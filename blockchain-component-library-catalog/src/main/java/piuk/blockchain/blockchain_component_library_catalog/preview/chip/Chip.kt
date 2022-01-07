package piuk.blockchain.blockchain_component_library_catalog.preview.chip

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.chip.Chip
import com.blockchain.componentlib.chip.ChipState
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "Enabled", group = "Chip")
@Composable
fun EnabledChipPreview() {
    val context = LocalContext.current
    AppTheme {
        AppSurface {
            Chip(
                text = "Enabled Chip",
                onClick = {
                    Toast.makeText(context, onChipClick(it), Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}


@Preview(name = "Disabled", group = "Chip")
@Composable
fun DisabledChipPreview() {
    val context = LocalContext.current
    AppTheme {
        AppSurface {
            Chip(
                text = "Disabled Chip",
                onClick = {
                    Toast.makeText(context, onChipClick(it), Toast.LENGTH_SHORT).show()
                },
                initialChipState = ChipState.Disabled
            )
        }
    }
}

@Preview(name = "Selected", group = "Chip")
@Composable
fun SelectedChipPreview() {
    val context = LocalContext.current
    AppTheme {
        AppSurface {
            Chip(
                text = "Selected Chip",
                onClick = {
                    Toast.makeText(context, onChipClick(it), Toast.LENGTH_SHORT).show()
                },
                initialChipState = ChipState.Selected
            )
        }
    }
}

private fun onChipClick(chipState: ChipState): String {
    return when (chipState) {
        ChipState.Enabled -> "Chip is enabled"
        ChipState.Selected -> "Chip is selected"
        ChipState.Disabled -> "Chip is disabled"
    }
}