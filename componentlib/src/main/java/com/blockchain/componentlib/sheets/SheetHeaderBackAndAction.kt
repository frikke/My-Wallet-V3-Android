package com.blockchain.componentlib.sheets

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.divider.HorizontalDivider
import com.blockchain.componentlib.image.Image
import com.blockchain.componentlib.image.ImageResource
import com.blockchain.componentlib.theme.AppSurface
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun SheetHeaderBackAndAction(
    title: String,
    onBackPress: () -> Unit,
    actionType: SheetHeaderActionType,
    onActionPress: () -> Unit,
    modifier: Modifier = Modifier,
    backPressContentDescription: String? = null,
    isDarkMode: Boolean = isSystemInDarkTheme(),
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

            Image(
                imageResource = ImageResource.Local(
                    id = if (isDarkMode) R.drawable.ic_back_chevron_dark else R.drawable.ic_back_chevron_light,
                    contentDescription = backPressContentDescription,
                ),
                modifier = Modifier
                    .clickable { onBackPress() }
                    .fillMaxHeight()
                    .size(24.dp),
                contentScale = ContentScale.None,
            )

            Text(
                text = title,
                style = AppTheme.typography.body2,
                color = AppTheme.colors.title,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )

            Text(
                text = actionType.value,
                style = AppTheme.typography.paragraph2,
                color = when (actionType) {
                    is SheetHeaderActionType.Cancel -> AppTheme.colors.error
                    is SheetHeaderActionType.Next -> AppTheme.colors.primary
                },
                modifier = Modifier.clickable { onActionPress() },
            )

            Spacer(Modifier.width(16.dp))
        }
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
    }
}


@Preview
@Composable
private fun SheetHeaderBackAndCancelPreview() {
    AppTheme {
        AppSurface {
            SheetHeaderBackAndAction(
                title = "Title",
                onBackPress = { /* no-op */ },
                actionType = SheetHeaderActionType.Cancel("Cancel"),
                onActionPress = { /* no-op */ }
            )
        }
    }
}

@Preview
@Composable
private fun SheetHeaderBackAndNextPreview() {
    AppTheme {
        AppSurface {
            SheetHeaderBackAndAction(
                title = "Title",
                onBackPress = { /* no-op */ },
                actionType = SheetHeaderActionType.Next("Next"),
                onActionPress = { /* no-op */ }
            )
        }
    }
}

sealed class SheetHeaderActionType {

    abstract val value: String

    data class Cancel(override val value: String) : SheetHeaderActionType()

    data class Next(override val value: String) : SheetHeaderActionType()
}

