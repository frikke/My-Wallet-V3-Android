package com.blockchain.componentlib.sheets

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Dark200
import com.blockchain.componentlib.theme.Grey600

@Composable
fun SheetHeaderBackAndClose(
    title: String,
    onBackPress: () -> Unit,
    onClosePress: () -> Unit,
    modifier: Modifier = Modifier,
    byline: String? = null,
    backPressContentDescription: String? = null,
    closePressContentDescription: String? = null,
) {

    Column(
        modifier = modifier
            .heightIn(48.dp)
            .height(IntrinsicSize.Max)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(12.dp))

            SheetHeaderBackButton(
                onBackPress = onBackPress,
                backPressContentDescription = backPressContentDescription,
                modifier = Modifier
                    .fillMaxHeight()
                    .size(24.dp)
            )

            SheetHeaderTitle(
                title = title,
                byline = byline,
                modifier = Modifier.weight(1f)
            )

            SheetHeaderCloseButton(
                onBackPress = onClosePress,
                backPressContentDescription = closePressContentDescription,
                modifier = Modifier.fillMaxHeight()
            )

            Spacer(Modifier.width(16.dp))
        }
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun SheetHeaderTitle(
    title: String,
    modifier: Modifier = Modifier,
    byline: String? = null,
    isDarkMode: Boolean = isSystemInDarkTheme(),
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = AppTheme.typography.body2,
            color = AppTheme.colors.title,
            textAlign = TextAlign.Center,
        )
        if (byline != null) {
            Text(
                text = byline,
                style = AppTheme.typography.caption1,
                color = if (isDarkMode) Dark200 else Grey600,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Preview
@Composable
private fun SheetHeaderBackAndClosePreview() {
    AppTheme {
        AppSurface {
            SheetHeaderBackAndClose(
                title = "Title",
                onBackPress = {/* no-op */ },
                onClosePress = {/* no-op */ },
            )
        }
    }
}

@Preview
@Composable
private fun SheetHeaderBackAndCloseBylinePreview() {
    AppTheme {
        AppSurface {
            SheetHeaderBackAndClose(
                title = "Title",
                onBackPress = {/* no-op */ },
                byline = "Byline",
                onClosePress = {/* no-op */ },
            )
        }
    }
}
