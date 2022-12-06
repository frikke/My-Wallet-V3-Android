package com.blockchain.componentlib.tag.button

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.theme.AppTheme
import com.blockchain.componentlib.theme.Grey400
import com.blockchain.componentlib.utils.clickableNoEffect

@Composable
fun TagButton(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        backgroundColor = if (selected) {
            AppTheme.colors.background
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
        Text(
            modifier = Modifier
                .padding(
                    horizontal = AppTheme.dimensions.verySmallSpacing,
                    vertical = AppTheme.dimensions.smallestSpacing
                )
                .clickableNoEffect(onClick),
            text = text,
            style = AppTheme.typography.paragraph2,
            color = if (selected) {
                AppTheme.colors.title
            } else {
                Grey400
            },
            textAlign = TextAlign.Center
        )

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