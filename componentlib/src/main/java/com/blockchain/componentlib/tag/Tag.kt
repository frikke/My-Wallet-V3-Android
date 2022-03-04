package com.blockchain.componentlib.tag

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.blockchain.componentlib.R
import com.blockchain.componentlib.theme.AppTheme

@Composable
fun Tag(text: String, size: TagSize, defaultBackgroundColor: Color, defaultTextColor: Color) {

    val paddingHorizontal = when (size) {
        TagSize.Primary -> 8.dp
        TagSize.Large -> dimensionResource(R.dimen.very_small_margin)
    }

    val paddingVertical = when (size) {
        TagSize.Primary -> dimensionResource(R.dimen.smallest_margin)
        TagSize.Large -> dimensionResource(R.dimen.minuscule_margin)
    }

    val textStyle = when (size) {
        TagSize.Primary -> AppTheme.typography.caption2
        TagSize.Large -> AppTheme.typography.paragraph2
    }

    Text(
        text = text,
        style = textStyle,
        color = defaultTextColor,
        modifier = Modifier
            .clip(AppTheme.shapes.small)
            .background(defaultBackgroundColor)
            .padding(horizontal = paddingHorizontal, vertical = paddingVertical)
    )
}
