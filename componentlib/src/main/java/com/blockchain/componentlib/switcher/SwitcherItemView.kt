package com.blockchain.componentlib.switcher

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.utils.BaseAbstractComposeView

class SwitcherItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseAbstractComposeView(context, attrs, defStyleAttr) {

    var text by mutableStateOf("")
    var startIcon: ImageResource by mutableStateOf(ImageResource.None)
    var endIcon: ImageResource by mutableStateOf(
        ImageResource.Local(
            contentDescription = "IconArrowRight",
            id = R.drawable.ic_arrow_right
        )
    )
    var switcherState by mutableStateOf(SwitcherState.Enabled)
    var onClick by mutableStateOf({})

    @Composable
    override fun Content() {
        AppTheme(setSystemColors = false) {
            AppSurface {
                SwitcherItem(
                    text = text,
                    state = switcherState,
                    startIcon = startIcon,
                    endIcon = endIcon,
                    onClick = onClick,
                )
            }
        }
    }
}

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
                startIcon = ImageResource.Local(
                    contentDescription = "Refresh",
                    id = R.drawable.ic_refresh
                ),
                endIcon = ImageResource.Local(
                    id = R.drawable.ic_arrow_right,
                    contentDescription = "IconArrowRight"
                ),
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
