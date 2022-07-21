package piuk.blockchain.blockchain_component_library_catalog.preview.switcher

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.switcher.SwitcherItem
import com.blockchain.componentlib.switcher.SwitcherState
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Preview(name = "switcher item enabled light mode", group = "Switcher", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun Switcher_preview_enabled_light_mode() {
    AppTheme {
        AppSurface {
            SwitcherItem(
                text = "One Time",
                state = SwitcherState.Enabled,
                startIcon = ImageResource.None,
                endIcon = ImageResource.Local(R.drawable.ic_arrow_right),
                onClick = { },
            )
        }
    }
}

@Preview(name = "switcher item disabled light mode", group = "Switcher", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun Switcher_preview_disabled_light_mode() {
    AppTheme {
        AppSurface {
            SwitcherItem(
                text = "One Time",
                state = SwitcherState.Disabled,
                startIcon = ImageResource.None,
                endIcon = ImageResource.Local(R.drawable.ic_arrow_right),
                onClick = { },
            )
        }
    }
}

@Preview(name = "switcher item enabled dark mode", group = "Switcher", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun Switcher_preview_enabled_dark_mode() {
    AppTheme {
        AppSurface {
            SwitcherItem(
                text = "One Time",
                state = SwitcherState.Enabled,
                startIcon = ImageResource.None,
                endIcon = ImageResource.Local(R.drawable.ic_arrow_right),
                onClick = { },
            )
        }
    }
}

@Preview(name = "switcher item disabled dark mode", group = "Switcher", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun Switcher_preview_disabled_dark_mode() {
    AppTheme {
        AppSurface {
            SwitcherItem(
                text = "One Time",
                state = SwitcherState.Disabled,
                startIcon = ImageResource.None,
                endIcon = ImageResource.Local(R.drawable.ic_arrow_right),
                onClick = { },
            )
        }
    }
}
