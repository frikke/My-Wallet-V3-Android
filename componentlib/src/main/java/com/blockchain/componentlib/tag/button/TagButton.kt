package com.blockchain.componentlib.tag.button

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.basic.Image
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.utils.clickableNoEffect

@Composable
fun TagButton(
    modifier: Modifier = Modifier,
    icon: ImageResource.Local? = null,
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        backgroundColor = if (selected) {
            AppTheme.colors.backgroundSecondary
        } else {
            Color.Transparent
        },
        shape = RoundedCornerShape(48.dp),
        elevation = if (selected) {
            AppTheme.dimensions.mediumElevation
        } else {
            0.dp
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppTheme.dimensions.verySmallSpacing,
                    vertical = AppTheme.dimensions.smallestSpacing
                )
                .clickableNoEffect(onClick),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Image(
                    imageResource = icon.withSize(AppTheme.dimensions.smallSpacing).withTint(
                        if (selected) {
                            AppTheme.colors.title
                        } else {
                            Grey400
                        }
                    )
                )
                Spacer(modifier = Modifier.size(AppTheme.dimensions.minusculeSpacing))
            }
            Text(
                text = text,
                style = AppTheme.typography.paragraph2,
                color = if (selected) {
                    AppTheme.colors.title
                } else {
                    AppTheme.colors.body
                },
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewTagButtonSelected() {
    TagButton(
        text = "Favorites",
        selected = true,
        onClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewTagButtonSelectedFalse() {
    TagButton(
        text = "Favorites",
        selected = false,
        onClick = {}
    )
}
