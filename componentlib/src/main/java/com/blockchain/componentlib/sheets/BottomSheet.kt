package com.blockchain.componentlib.sheets

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.DestructiveOutlinedButton
import com.blockchain.componentlib.button.PrimaryOutlinedButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.button.SmallOutlinedButton
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark800

@Composable
fun BottomSheetTwoButtons(
    onCloseClick: () -> Unit,
    headerImageResource: ImageResource?,
    title: String,
    showTitleInHeader: Boolean = false,
    subtitle: String = "",
    button1: BottomSheetButton,
    button2: BottomSheetButton,
    subtitleAlign: TextAlign = TextAlign.Center,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    shouldShowHeaderDivider: Boolean = true
) {
    BottomSheet(
        onCloseClick = onCloseClick,
        headerImageResource = headerImageResource,
        title = title,
        subtitleAlign = subtitleAlign,
        showTitleInHeader = showTitleInHeader,
        subtitle = subtitle,
        buttonsContent = {
            button1.toBottomSheetButtonComposable(Modifier.wrapContentWidth()).invoke()
            button2.toBottomSheetButtonComposable(Modifier.wrapContentWidth()).invoke()
        },
        isDarkTheme = isDarkTheme,
        shouldShowHeaderDivider = shouldShowHeaderDivider
    )
}

@Composable
fun BottomSheetOneButton(
    onCloseClick: () -> Unit,
    headerImageResource: ImageResource?,
    title: String,
    showTitleInHeader: Boolean = false,
    subtitle: String = "",
    subtitleAlign: TextAlign = TextAlign.Center,
    button: BottomSheetButton,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    shouldShowHeaderDivider: Boolean = true
) {
    BottomSheet(
        onCloseClick = onCloseClick,
        headerImageResource = headerImageResource,
        title = title,
        subtitleAlign = subtitleAlign,
        showTitleInHeader = showTitleInHeader,
        subtitle = subtitle,
        buttonsContent = {
            button.toBottomSheetButtonComposable(Modifier.wrapContentWidth()).invoke()
        },
        isDarkTheme = isDarkTheme,
        shouldShowHeaderDivider = shouldShowHeaderDivider
    )
}

@Composable
fun BottomSheetNoButtons(
    onCloseClick: () -> Unit,
    headerImageResource: ImageResource?,
    title: String,
    textAlign: TextAlign = TextAlign.Center,
    showTitleInHeader: Boolean = false,
    subtitle: String = "",
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    shouldShowHeaderDivider: Boolean = true
) {
    BottomSheet(
        onCloseClick = onCloseClick,
        headerImageResource = headerImageResource,
        title = title,
        subtitleAlign = textAlign,
        showTitleInHeader = showTitleInHeader,
        subtitle = subtitle,
        buttonsContent = null,
        isDarkTheme = isDarkTheme,
        shouldShowHeaderDivider = shouldShowHeaderDivider
    )
}

@Composable
private fun BottomSheet(
    onCloseClick: () -> Unit,
    headerImageResource: ImageResource?,
    title: String,
    subtitleAlign: TextAlign = TextAlign.Center,
    showTitleInHeader: Boolean = false,
    subtitle: String = "",
    buttonsContent: (@Composable ColumnScope.() -> Unit)? = null,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    shouldShowHeaderDivider: Boolean = true
) {
    val backgroundColor = if (!isDarkTheme) {
        Color.White
    } else {
        Dark800
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                backgroundColor,
                RoundedCornerShape(dimensionResource(id = com.blockchain.componentlib.R.dimen.tiny_spacing))
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SheetHeader(
            title = title.takeIf { showTitleInHeader },
            onClosePress = onCloseClick,
            shouldShowDivider = shouldShowHeaderDivider
        )
        Spacer(Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing)))
        if (headerImageResource != null) {
            Image(
                imageResource = headerImageResource,
                modifier = Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.size_huge))
            )
            Spacer(Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing)))
        }

        if (!showTitleInHeader) {
            Text(
                text = title,
                style = AppTheme.typography.title3,
                color = AppTheme.colors.title
            )
        }
        if (subtitle.isNotEmpty()) {
            Spacer(Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.tiny_spacing)))
            Text(
                text = subtitle,
                style = AppTheme.typography.paragraph1,
                textAlign = subtitleAlign,
                color = AppTheme.colors.title,
                modifier = Modifier.padding(
                    start = dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing),
                    end = dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing)
                )
            )
        }

        Spacer(
            Modifier.size(
                if (buttonsContent == null) {
                    dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing)
                } else {
                    dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing)
                }
            )
        )

        if (buttonsContent != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = dimensionResource(com.blockchain.componentlib.R.dimen.standard_spacing)),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = buttonsContent
            )
            Spacer(Modifier.size(dimensionResource(com.blockchain.componentlib.R.dimen.small_spacing)))
        }
    }
}

@Composable
private fun BottomSheetButton.toBottomSheetButtonComposable(modifier: Modifier): @Composable (() -> Unit) {
    return {
        when (type) {
            ButtonType.PRIMARY -> PrimaryButton(
                text = text,
                onClick = onClick,
                modifier = modifier.fillMaxWidth()
            )
            ButtonType.MINIMAL -> PrimaryOutlinedButton(
                text = text,
                onClick = onClick,
                modifier = modifier.fillMaxWidth()
            )
            ButtonType.SMALL_MINIMAL -> SmallOutlinedButton(
                text = text,
                onClick = onClick,
                modifier = modifier
            )
            ButtonType.DESTRUCTIVE_MINIMAL -> DestructiveOutlinedButton(
                text = text,
                onClick = onClick,
                modifier = modifier.fillMaxWidth()
            )
        }
    }
}

data class BottomSheetButton(
    val type: ButtonType,
    val onClick: () -> Unit,
    val text: String
)

enum class ButtonType {
    PRIMARY, MINIMAL, DESTRUCTIVE_MINIMAL, SMALL_MINIMAL
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun NoButtonBottomSheet() {
    AppTheme {
        AppSurface {
            BottomSheetNoButtons(
                onCloseClick = {},
                title = "NoButtonBottomSheet",
                headerImageResource = ImageResource.None,
                subtitle = " NoButtonBottomSheetSubtitle"
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun OnlyPrimaryTopButtonBottomSheet() {
    AppTheme {
        AppSurface {
            BottomSheetOneButton(
                onCloseClick = {},
                title = "NoButtonBottomSheet",
                headerImageResource = ImageResource.Local(R.drawable.ic_blockchain),
                subtitle = " NoButtonBottomSheetSubtitle",
                button = BottomSheetButton(
                    type = ButtonType.PRIMARY,
                    onClick = {},
                    text = "OK"
                )
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnlyPrimaryTopButtonBottomSheetWithNoSubtitle() {
    AppTheme {
        AppSurface {
            BottomSheetOneButton(
                onCloseClick = {},
                title = "NoButtonBottomSheet",
                headerImageResource = ImageResource.Local(R.drawable.ic_blockchain),
                button = BottomSheetButton(
                    type = ButtonType.PRIMARY,
                    onClick = {},
                    text = "OK"
                )
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TopAndBottomButtonsSheet() {
    AppTheme {
        AppSurface {
            BottomSheetTwoButtons(
                onCloseClick = {},
                title = "NoButtonBottomSheet",
                headerImageResource = ImageResource.Local(R.drawable.ic_blockchain),
                subtitle = "NoButtonBottomSheetSubtitle",
                button1 = BottomSheetButton(
                    type = ButtonType.PRIMARY,
                    onClick = {},
                    text = "OK"
                ),
                button2 = BottomSheetButton(
                    type = ButtonType.DESTRUCTIVE_MINIMAL,
                    onClick = {},
                    text = "Cancel"
                )
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PrimaryAndMinimalButtonSheet() {
    AppTheme {
        AppSurface {
            BottomSheetTwoButtons(
                onCloseClick = {},
                title = "NoButtonBottomSheet",
                headerImageResource = ImageResource.Local(R.drawable.ic_blockchain),
                subtitle = "NoButtonBottomSheetSubtitle",
                button1 = BottomSheetButton(
                    type = ButtonType.MINIMAL,
                    onClick = {},
                    text = "Learn More"
                ),
                button2 = BottomSheetButton(
                    type = ButtonType.PRIMARY,
                    onClick = {},
                    text = "Ok"
                )
            )
        }
    }
}

@Preview()
@Composable
fun PrimaryAndSmallMinimalButtonSheet() {
    AppTheme {
        AppSurface {
            BottomSheetTwoButtons(
                onCloseClick = {},
                title = "NoButtonBottomSheet",
                headerImageResource = ImageResource.Local(R.drawable.ic_blockchain),
                subtitle = "NoButtonBottomSheetSubtitle",
                button1 = BottomSheetButton(
                    type = ButtonType.SMALL_MINIMAL,
                    onClick = {},
                    text = "Learn More"
                ),
                button2 = BottomSheetButton(
                    type = ButtonType.PRIMARY,
                    onClick = {},
                    text = "Ok"
                )
            )
        }
    }
}
