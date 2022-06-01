package com.blockchain.componentlib.sheets

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark200
import com.blockchain.componentlib.theme.Grey600

@Composable
fun SheetHeader(
    modifier: Modifier = Modifier,
    title: String? = null,
    byline: String? = null,
    startImageResource: ImageResource = ImageResource.None,
    onClosePress: (() -> Unit)? = null,
    closePressContentDescription: String? = null,
    shouldShowDivider: Boolean = true,
) {
    Box(
        modifier = modifier
    ) {
        SheetNub(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
        )
        Column {
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.width(dimensionResource(R.dimen.standard_margin)))

                if (startImageResource != ImageResource.None) {
                    Image(
                        imageResource = startImageResource,
                        modifier = Modifier
                            .padding(top = dimensionResource(R.dimen.standard_margin))
                            .size(28.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }

                SheetHeaderTitle(
                    title = title,
                    byline = byline,
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            top = dimensionResource(R.dimen.standard_margin),
                            bottom = if (byline.isNullOrBlank()) 10.dp else 5.dp
                        )
                )

                onClosePress?.let {
                    SheetHeaderCloseButton(
                        onClosePress = onClosePress,
                        backPressContentDescription = closePressContentDescription,
                        modifier = Modifier.padding(
                            top = dimensionResource(R.dimen.medium_margin),
                            end = dimensionResource(R.dimen.medium_margin)
                        )
                    )
                }
            }
            if (shouldShowDivider) {
                HorizontalDivider(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun SheetHeaderTitle(
    modifier: Modifier = Modifier,
    title: String?,
    byline: String? = null,
    isDarkMode: Boolean = isSystemInDarkTheme(),
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
    ) {
        title?.let {
            Text(
                text = title,
                style = AppTheme.typography.title3,
                color = AppTheme.colors.title,
                textAlign = TextAlign.Center,
            )
        }

        if (byline != null && byline.isNotBlank()) {
            Text(
                text = byline,
                style = AppTheme.typography.paragraph1,
                color = if (isDarkMode) Dark200 else Grey600,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview
@Composable
private fun SheetHeaderPreview() {
    AppTheme {
        AppSurface {
            SheetHeader(
                title = "Title",
                onClosePress = { /* no-op */ },
            )
        }
    }
}

@Preview
@Composable
private fun SheetHeaderNoTitle() {
    AppTheme {
        AppSurface {
            SheetHeader(
                onClosePress = { /* no-op */ },
            )
        }
    }
}

@Preview
@Composable
private fun SheetHeaderNoTitleNoClose() {
    AppTheme {
        AppSurface {
            SheetHeader(shouldShowDivider = false)
        }
    }
}

@Preview
@Composable
private fun SheetHeaderBylinePreview() {
    AppTheme {
        AppSurface {
            SheetHeader(
                title = "Title",
                byline = "Byline",
                onClosePress = { /* no-op */ },
            )
        }
    }
}

@Preview
@Composable
private fun SheetHeaderWithStartIconPreview() {
    AppTheme {
        AppSurface {
            SheetHeader(
                title = "Title",
                onClosePress = { /* no-op */ },
                startImageResource = ImageResource.Local(
                    id = R.drawable.ic_qr_code,
                    contentDescription = null,
                ),
            )
        }
    }
}

@Preview
@Composable
private fun SheetHeaderBylineWithStartIconPreview() {
    AppTheme {
        AppSurface {
            SheetHeader(
                title = "Title",
                byline = "Byline",
                onClosePress = { /* no-op */ },
                startImageResource = ImageResource.Local(
                    id = R.drawable.ic_qr_code,
                    contentDescription = null,
                ),
            )
        }
    }
}
