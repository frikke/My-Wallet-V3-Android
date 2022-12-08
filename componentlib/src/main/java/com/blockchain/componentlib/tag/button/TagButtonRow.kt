package com.blockchain.componentlib.tag.button

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.blockchain.componentlib.theme.AppTheme

data class TagButtonValue<T>(
    val obj: T,
    val stringVal: String
)

@Composable
fun <T> TagButtonRow(
    modifier: Modifier = Modifier,
    selected: T,
    values: List<TagButtonValue<T>>,
    spaceBetween: Dp = AppTheme.dimensions.tinySpacing,
    onClick: (T) -> Unit
) {
    Row(modifier = modifier) {
        values.forEachIndexed { index, value ->
            TagButton(
                modifier = Modifier.weight(1F),
                text = value.stringVal,
                selected = value.obj == selected,
                onClick = { onClick(value.obj) }
            )

            if (index < values.lastIndex) {
                Spacer(modifier = Modifier.size(spaceBetween))
            }
        }
    }
}

@Preview
@Composable
fun PreviewTagButtonRow() {
    var selected by remember { mutableStateOf("one") }
    TagButtonRow(
        selected = selected,
        values = listOf("one", "two", "three").map { TagButtonValue(it, it) },
        onClick = { selected = it }
    )
}
