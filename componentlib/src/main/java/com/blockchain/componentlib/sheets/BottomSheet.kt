package com.blockchain.componentlib.sheets

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.button.DestructiveMinimalButton
import com.blockchain.componentlib.button.DestructivePrimaryButton
import com.blockchain.componentlib.button.MinimalButton
import com.blockchain.componentlib.button.PrimaryButton
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark800

@Composable
fun BottomSheet(
    onCloseClick: () -> Unit,
    imageResource: ImageResource,
    title: String,
    subtitle: String = "",
    topButton: BottomSheetButton? = null,
    bottomButton: BottomSheetButton? = null,
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
            .background(backgroundColor),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SheetHeader(
            onClosePress = onCloseClick,
            shouldShowDivider = shouldShowHeaderDivider
        )
        Spacer(Modifier.size(dimensionResource(R.dimen.small_margin)))
        Image(
            imageResource = imageResource,
            modifier = Modifier.size(dimensionResource(R.dimen.size_huge))
        )
        Spacer(Modifier.size(dimensionResource(R.dimen.small_margin)))

        Text(
            text = title,
            style = AppTheme.typography.title3,
            color = AppTheme.colors.title,
        )
        if (subtitle.isNotEmpty()) {
            Spacer(Modifier.size(dimensionResource(R.dimen.tiny_margin)))
            Text(
                text = subtitle,
                style = AppTheme.typography.paragraph1,
                textAlign = TextAlign.Center,
                color = AppTheme.colors.title,
                modifier = Modifier.padding(
                    start = dimensionResource(R.dimen.standard_margin),
                    end = dimensionResource(R.dimen.standard_margin)
                )
            )
        }

        val noButtons = topButton == null && bottomButton == null

        Spacer(
            Modifier.size(
                if (noButtons)
                    dimensionResource(R.dimen.small_margin)
                else
                    dimensionResource(R.dimen.standard_margin)
            )
        )

        topButton?.let {
            it.toBottomSheetButtonComposable().invoke(this)
            Spacer(Modifier.size(dimensionResource(R.dimen.small_margin)))
        }

        bottomButton?.let {
            it.toBottomSheetButtonComposable().invoke(this)
            Spacer(Modifier.size(dimensionResource(R.dimen.small_margin)))
        }
    }
}

@Composable
private fun BottomSheetButton.toBottomSheetButtonComposable(): @Composable (ColumnScope.() -> Unit) {
    val modifier = Modifier
        .fillMaxWidth()
        .padding(
            start = dimensionResource(R.dimen.standard_margin),
            end = dimensionResource(R.dimen.standard_margin)
        )
    return {
        when (type) {
            ButtonType.PRIMARY -> PrimaryButton(
                text = text,
                onClick = onClick,
                modifier = modifier
            )
            ButtonType.MINIMAL -> MinimalButton(
                text = text,
                onClick = onClick,
                modifier = modifier
            )
            ButtonType.DESTRUCTIVE_MINIMAL -> DestructiveMinimalButton(
                text = text,
                onClick = onClick,
                modifier = modifier
            )
            ButtonType.DESTRUCTIVE_PRIMARY ->
                DestructivePrimaryButton(
                    text = text,
                    onClick = onClick,
                    modifier = modifier
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
    PRIMARY, MINIMAL, DESTRUCTIVE_MINIMAL, DESTRUCTIVE_PRIMARY
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun NoButtonBottomSheet() {
    AppTheme {
        AppSurface {
            BottomSheet(
                onCloseClick = {},
                title = "NoButtonBottomSheet",
                imageResource = ImageResource.None,
                subtitle = " NoButtonBottomSheetSubtitle",
                topButton = null,
                bottomButton = null
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun OnlyPrimaryTopButtonBottomSheet() {
    AppTheme {
        AppSurface {
            BottomSheet(
                onCloseClick = {},
                title = "NoButtonBottomSheet",
                imageResource = ImageResource.Local(R.drawable.ic_blockchain),
                subtitle = " NoButtonBottomSheetSubtitle",
                topButton = BottomSheetButton(type = ButtonType.PRIMARY, onClick = {}, text = "OK"),
                bottomButton = null
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun OnlyPrimaryTopButtonBottomSheetWithNoSubtitle() {
    AppTheme {
        AppSurface {
            BottomSheet(
                onCloseClick = {},
                title = "NoButtonBottomSheet",
                imageResource = ImageResource.Local(R.drawable.ic_blockchain),
                topButton = BottomSheetButton(type = ButtonType.PRIMARY, onClick = {}, text = "OK"),
                bottomButton = null
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TopAndBottomButtonsSheet() {
    AppTheme {
        AppSurface {
            BottomSheet(
                onCloseClick = {},
                title = "NoButtonBottomSheet",
                imageResource = ImageResource.Local(R.drawable.ic_blockchain),
                subtitle = "NoButtonBottomSheetSubtitle",
                topButton = BottomSheetButton(type = ButtonType.PRIMARY, onClick = {}, text = "OK"),
                bottomButton = BottomSheetButton(type = ButtonType.DESTRUCTIVE_MINIMAL, onClick = {}, text = "Cancel")
            )
        }
    }
}
