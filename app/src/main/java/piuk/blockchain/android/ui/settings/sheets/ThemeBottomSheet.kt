package piuk.blockchain.android.ui.settings.sheets

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.commonarch.presentation.base.ThemedBottomSheetFragment
import com.blockchain.componentlib.basic.AppDivider
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.icons.Check
import com.blockchain.componentlib.icons.Icons
import com.blockchain.componentlib.icons.Moon
import com.blockchain.componentlib.icons.Settings
import com.blockchain.componentlib.icons.Sun
import com.blockchain.componentlib.sheets.SheetHeader
import com.blockchain.componentlib.theme.AppColors
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.topOnly
import com.blockchain.stringResources.R
import com.blockchain.theme.Theme
import com.blockchain.theme.ThemeService
import org.koin.android.ext.android.inject

class ThemeBottomSheet : ThemedBottomSheetFragment() {

    private val themeService: ThemeService by inject()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val selectedTheme = themeService.currentTheme()

        return ComposeView(requireContext()).apply {
            setContent {
                ThemeScreen(
                    selectedTheme = selectedTheme,
                    closeOnClick = this@ThemeBottomSheet::dismiss,
                    onClick = {
                        themeService.setTheme(it)
                        dismiss()
                    }
                )
            }
        }
    }

    companion object {
        fun newInstance(): ThemeBottomSheet {
            return ThemeBottomSheet()
        }
    }
}

@Composable
private fun ThemeScreen(
    selectedTheme: Theme,
    closeOnClick: () -> Unit,
    onClick: (Theme) -> Unit
) {
    Surface(
        color = AppColors.background,
        shape = AppTheme.shapes.large.topOnly()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SheetHeader(
                title = stringResource(id = R.string.settings_theme_title),
                onClosePress = closeOnClick
            )

            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))

            Surface(
                modifier = Modifier.padding(AppTheme.dimensions.smallSpacing),
                shape = AppTheme.shapes.large,
                color = Color.Transparent
            ) {
                Column {
                    Theme.values().forEachIndexed { index, theme ->
                        ThemeItem(
                            name = stringResource(theme.textResource()),
                            icon = theme.icon(),
                            selected = selectedTheme == theme,
                            onClick = { onClick(theme) }
                        )

                        if (index < Theme.values().lastIndex) {
                            AppDivider()
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))
        }
    }
}

@Composable
private fun ThemeItem(
    name: String,
    icon: ImageResource.Local,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.backgroundSecondary)
            .clickable(onClick = onClick)
            .padding(AppTheme.dimensions.smallSpacing)
    ) {
        Image(imageResource = icon.withTint(AppTheme.colors.title))
        Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))
        Text(
            modifier = Modifier.weight(1F),
            text = name,
            style = AppTheme.typography.paragraph2,
            color = AppTheme.colors.title
        )
        if (selected) {
            Spacer(modifier = Modifier.size(AppTheme.dimensions.smallSpacing))
            Image(imageResource = Icons.Check.withTint(AppTheme.colors.primary))
        }
    }
}

@StringRes fun Theme.textResource() = when (this) {
    Theme.LightMode -> R.string.settings_theme_light
    Theme.DarkMode -> R.string.settings_theme_dark
    Theme.System -> R.string.settings_theme_system
}

fun Theme.icon() = when (this) {
    Theme.LightMode -> Icons.Sun
    Theme.DarkMode -> Icons.Moon
    Theme.System -> Icons.Settings
}

@Preview
@Composable
private fun PreviewThemeScreen() {
    ThemeScreen(
        selectedTheme = Theme.LightMode,
        {}, {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewThemeScreenDark() {
    PreviewThemeScreen()
}
